package com.islehub.user.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.islehub.common.enums.RCode;
import com.islehub.common.exception.BizException;
import com.islehub.common.redis.RedisKeys;
import com.islehub.common.result.Result;
import com.islehub.user.dto.UserAddDTO;
import com.islehub.user.dto.UserRegisterDTO;
import com.islehub.user.dto.UserUpdateDTO;
import com.islehub.user.entity.User;
import com.islehub.user.mapper.UserMapper;
import com.islehub.user.service.UserService;
import com.islehub.user.util.EmailCheckUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final StringRedisTemplate stringRedisTemplate;
    private final EmailCheckUtil emailCheckUtil;
    private final HttpServletRequest request;

    /**
     * 原子化登录失败计数 + 锁定。
     * KEYS[1]=failKey, KEYS[2]=lockKey
     * ARGV[1]=failTtl(秒), ARGV[2]=lockTtl(秒), ARGV[3]=threshold
     * 返回: -1=已锁定, >=0=当前失败次数
     */
    private static final DefaultRedisScript<Long> LOGIN_FAIL_SCRIPT;

    static {
        LOGIN_FAIL_SCRIPT = new DefaultRedisScript<>();
        LOGIN_FAIL_SCRIPT.setResultType(Long.class);
        LOGIN_FAIL_SCRIPT.setScriptText(
            "local count = redis.call('INCR', KEYS[1]); " +
            "if count == 1 then " +
            "  redis.call('EXPIRE', KEYS[1], ARGV[1]); " +
            "end; " +
            "if count >= tonumber(ARGV[3]) then " +
            "  redis.call('SET', KEYS[2], '1', 'EX', ARGV[2]); " +
            "  redis.call('DEL', KEYS[1]); " +
            "  return -1; " +
            "end; " +
            "return count;"
        );
    }

    @Override
    public Result<String> login(String account, String password) {
        if (!StringUtils.hasText(account) || !StringUtils.hasText(password)) {
            return Result.fail(RCode.BAD_REQUEST, "邮箱/用户名和密码不能为空");
        }
        String lockKey = RedisKeys.loginLock(account);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKey))) {
            return Result.fail(RCode.TOO_MANY_REQUESTS, "账号已锁定2分钟，请稍后再试");
        }
        boolean isEmail = account.contains("@");
        User user = isEmail
                ? lambdaQuery().eq(User::getEmail, account).one()
                : lambdaQuery().eq(User::getUsername, account).one();
        if (user == null) {
            return Result.fail(RCode.UNAUTHORIZED, "账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() == 0) {
            return Result.fail(RCode.FORBIDDEN, "账号已被禁用");
        }
        if (!BCrypt.checkpw(password, user.getPassword())) {
            String failKey = RedisKeys.loginFail(account);
            Long result = stringRedisTemplate.execute(LOGIN_FAIL_SCRIPT,
                    List.of(failKey, lockKey),
                    String.valueOf(RedisKeys.LOGIN_FAIL_WINDOW.getSeconds()),
                    String.valueOf(RedisKeys.LOGIN_LOCK_TTL.getSeconds()),
                    "3");
            if (result != null && result == -1) {
                return Result.fail(RCode.TOO_MANY_REQUESTS, "账号已锁定2分钟，请稍后再试");
            }
            return Result.fail(RCode.UNAUTHORIZED, "账号或密码错误");
        }
        StpUtil.login(user.getId());
        StpUtil.getSession().set("user", user);
        stringRedisTemplate.delete(RedisKeys.loginFail(account));
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(request.getRemoteAddr());
        updateById(user);
        return Result.ok(StpUtil.getTokenValue());
    }

    @Override
    public void sendEmailCode(String email) {
        // 邮箱已注册则拒绝
        if (lambdaQuery().eq(User::getEmail, email).one() != null) {
            throw new BizException("该邮箱已被注册");
        }
        // 原子检查+设置频率限制（SET NX EX）
        String rateKey = RedisKeys.emailRateLimit(email);
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(rateKey, "1", RedisKeys.EMAIL_RATE_LIMIT_TTL);
        if (Boolean.FALSE.equals(acquired)) {
            throw new BizException(RCode.TOO_MANY_REQUESTS.getCode(), "验证码发送过于频繁，请60秒后再试");
        }

        String code = emailCheckUtil.generateCode();
        // 先存 Redis，再发邮件 — 避免邮件发出但验证码未落库
        stringRedisTemplate.opsForValue()
                .set(RedisKeys.emailVerifyCode(email), code, RedisKeys.EMAIL_CODE_TTL);

        try {
            emailCheckUtil.sendCode(email, code);
        } catch (Exception e) {
            stringRedisTemplate.delete(RedisKeys.emailVerifyCode(email));
            throw new BizException(emailCheckUtil.errorMatcher(e.getMessage()));
        }
    }

    @Override
    public Page<User> pageUsers(int page, int pageSize, String keyword) {
        return baseMapper.pageUsers(new Page<>(page, pageSize), keyword);
    }

    @Override
    public void addUser(UserAddDTO dto) {
        if (lambdaQuery().eq(User::getUsername, dto.getUsername()).one() != null) {
            throw new BizException("用户名已存在");
        }
        if (lambdaQuery().eq(User::getEmail, dto.getEmail()).one() != null) {
            throw new BizException("该邮箱已被注册");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setStatus(dto.getStatus());
        try {
            save(user);
        } catch (DuplicateKeyException e) {
            throw translateDuplicateKey(e);
        }
    }

    @Override
    public void register(UserRegisterDTO dto) {
        String codeKey = RedisKeys.emailVerifyCode(dto.getEmail());
        String savedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (savedCode == null) {
            throw new BizException("验证码已过期，请重新发送");
        }
        if (!savedCode.equals(dto.getEmailConfirmCode())) {
            throw new BizException("验证码错误");
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BizException("两次输入的密码不一致");
        }
        if (lambdaQuery().eq(User::getEmail, dto.getEmail()).one() != null) {
            throw new BizException("该邮箱已被注册");
        }
        if (lambdaQuery().eq(User::getUsername, dto.getUsername()).one() != null) {
            throw new BizException("用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setRole("customer");
        user.setStatus(1);
        try {
            save(user);
        } catch (DuplicateKeyException e) {
            throw translateDuplicateKey(e);
        }
        stringRedisTemplate.delete(codeKey);
    }

    @Override
    public void updateUser(Long id, UserUpdateDTO dto) {
        if (StringUtils.hasText(dto.getUsername())) {
            User exist = lambdaQuery().eq(User::getUsername, dto.getUsername())
                    .ne(User::getId, id).one();
            if (exist != null) {
                throw new BizException("用户名已存在");
            }
        }
        if (lambdaQuery().eq(User::getEmail, dto.getEmail())
                .ne(User::getId, id).one() != null) {
            throw new BizException("该邮箱已被注册");
        }
        User user = new User();
        user.setId(id);
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setAvatar(dto.getAvatar());
        user.setStatus(dto.getStatus());
        try {
            updateById(user);
        } catch (DuplicateKeyException e) {
            throw translateDuplicateKey(e);
        }
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException("状态值只能为 0 或 1");
        }
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        updateById(user);
    }

    /** 将 DB 唯一约束冲突转为友好中文提示 */
    private BizException translateDuplicateKey(DuplicateKeyException e) {
        String msg = e.getMessage();
        if (msg != null) {
            if (msg.contains("uk_email")) {
                return new BizException("该邮箱已被注册");
            }
            if (msg.contains("uk_username")) {
                return new BizException("用户名已存在");
            }
        }
        return new BizException("数据已存在，请检查后重试");
    }
}
