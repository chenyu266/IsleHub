package com.islehub.user.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.islehub.common.enums.RCode;
import com.islehub.common.exception.BizException;
import com.islehub.common.redis.RedisKeys;
import com.islehub.common.result.Result;
import com.islehub.user.dto.UserRegisterDTO;
import com.islehub.user.entity.User;
import com.islehub.user.mapper.UserMapper;
import com.islehub.user.service.UserService;
import com.islehub.user.util.EmailCheckUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final StringRedisTemplate stringRedisTemplate;
    private final EmailCheckUtil emailCheckUtil;

    @Override
    public Result<String> login(String account, String password) {
        if (!StringUtils.hasText(account) || !StringUtils.hasText(password)) {
            return Result.fail(RCode.BAD_REQUEST, "邮箱/用户名和密码不能为空");
        }
        // 1. 检查是否被锁定
        String lockKey = RedisKeys.loginLock(account);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKey))) {
            return Result.fail(RCode.TOO_MANY_REQUESTS, "账号已锁定2分钟，请稍后再试");
        }

        // 2. 邮箱/用户名识别 → 查询用户
        boolean isEmail = account.contains("@");
        User user = isEmail
                ? lambdaQuery().eq(User::getEmail, account).one()
                : lambdaQuery().eq(User::getUsername, account).one();

        // 3. 用户不存在或已禁用
        if (user == null) {
            return Result.fail(RCode.UNAUTHORIZED, "邮箱或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() == 0) {
            return Result.fail(RCode.FORBIDDEN, "账号已被禁用");
        }

        // 4. 密码校验
        if (!BCrypt.checkpw(password, user.getPassword())) {
            String failKey = RedisKeys.loginFail(account);
            Long failCount = stringRedisTemplate.opsForValue().increment(failKey);
            if (failCount != null && failCount == 1) {
                stringRedisTemplate.expire(failKey, RedisKeys.LOGIN_FAIL_WINDOW);
            }
            if (failCount != null && failCount >= 3) {
                stringRedisTemplate.opsForValue()
                        .set(lockKey, "1", RedisKeys.LOGIN_LOCK_TTL);
                stringRedisTemplate.delete(failKey);
                return Result.fail(RCode.TOO_MANY_REQUESTS, "账号已锁定2分钟，请稍后再试");
            }
            return Result.fail(RCode.UNAUTHORIZED, "邮箱或密码错误");
        }

        // 5. 登录成功
        StpUtil.login(user.getId());
        StpUtil.getSession().set("user", user);
        stringRedisTemplate.delete(RedisKeys.loginFail(account));
        user.setLastLoginTime(LocalDateTime.now());
        updateById(user);
        return Result.ok(StpUtil.getTokenValue());
    }

    @Override
    public void sendEmailCode(String email) {
        // 60s 发送频率限制
        String rateKey = RedisKeys.emailRateLimit(email);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(rateKey))) {
            throw new BizException(RCode.TOO_MANY_REQUESTS.getCode(), "验证码发送过于频繁，请60秒后再试");
        }
        String code = emailCheckUtil.generateCode();
        try {
            emailCheckUtil.sendCode(email, code);
        } catch (Exception e) {
            throw new BizException(emailCheckUtil.errorMatcher(e.getMessage()));
        }
        stringRedisTemplate.opsForValue().set(rateKey, "1", RedisKeys.EMAIL_RATE_LIMIT_TTL);
        stringRedisTemplate.opsForValue()
                .set(RedisKeys.emailVerifyCode(email), code, RedisKeys.EMAIL_CODE_TTL);
    }

    @Override
    public Page<User> pageUsers(int page, int pageSize, String keyword) {
        return baseMapper.pageUsers(new Page<>(page, pageSize), keyword);
    }

    @Override
    public void addUser(User user) {
        if (!StringUtils.hasText(user.getPassword())) {
            throw new BizException("密码不能为空");
        }
        User exist = lambdaQuery().eq(User::getUsername, user.getUsername()).one();
        if (exist != null) {
            throw new BizException("用户名已存在");
        }
        user.setPassword(BCrypt.hashpw(user.getPassword()));
        save(user);
    }

    @Override
    public void register(UserRegisterDTO dto) {
        // 校验验证码
        String codeKey = RedisKeys.emailVerifyCode(dto.getEmail());
        String savedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (savedCode == null) {
            throw new BizException("验证码已过期，请重新发送");
        }
        if (!savedCode.equals(dto.getEmailConfirmCode())) {
            throw new BizException("验证码错误");
        }

        // 校验确认密码
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BizException("两次输入的密码不一致");
        }

        // 邮箱唯一性
        if (lambdaQuery().eq(User::getEmail, dto.getEmail()).one() != null) {
            throw new BizException("该邮箱已被注册");
        }

        // 用户名唯一性
        if (lambdaQuery().eq(User::getUsername, dto.getUsername()).one() != null) {
            throw new BizException("用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setRole("customer");
        user.setStatus(1);
        save(user);

        // 注册成功，删除验证码
        stringRedisTemplate.delete(codeKey);
    }

    @Override
    public void updateUser(User user) {
        User exist = lambdaQuery().eq(User::getUsername, user.getUsername())
                .ne(User::getId, user.getId()).one();
        if (exist != null) {
            throw new BizException("用户名已存在");
        }
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(BCrypt.hashpw(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        updateById(user);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        updateById(user);
    }
}
