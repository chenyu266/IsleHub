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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final Set<String> ALLOWED_IMAGE_EXTS = Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp");

    @Value("${upload.path:uploads}")
    private String uploadPath;

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
    public void sendChangeEmailCode(Long userId, String newEmail) {
        User user = getById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (newEmail.equals(user.getEmail())) {
            throw new BizException("新邮箱不能与当前邮箱相同");
        }
        if (lambdaQuery().eq(User::getEmail, newEmail)
                .ne(User::getId, userId).one() != null) {
            throw new BizException("该邮箱已被其他账号使用");
        }
        // 向当前邮箱发送验证码
        sendCodeToEmail(user.getEmail(), RedisKeys.emailChangeCode(user.getEmail()));
    }

    @Override
    public void changeEmail(Long userId, String newEmail, String oldCode, String newCode) {
        User user = getById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }

        if (newEmail.equals(user.getEmail())) {
            throw new BizException("新邮箱不能与当前邮箱相同");
        }

        // 验证旧邮箱验证码
        String oldEmail = user.getEmail();
        String savedOldCode = stringRedisTemplate.opsForValue()
                .get(RedisKeys.emailChangeCode(oldEmail));
        if (savedOldCode == null) {
            throw new BizException("旧邮箱验证码已过期，请重新发送");
        }
        if (!savedOldCode.equals(oldCode)) {
            throw new BizException("旧邮箱验证码错误");
        }

        // 验证新邮箱验证码（独立 key，与注册验证码隔离）
        String savedNewCode = stringRedisTemplate.opsForValue()
                .get(RedisKeys.emailChangeNewCode(newEmail));
        if (savedNewCode == null) {
            throw new BizException("新邮箱验证码已过期，请重新发送");
        }
        if (!savedNewCode.equals(newCode)) {
            throw new BizException("新邮箱验证码错误");
        }

        // 二次确认新邮箱未被占用
        if (lambdaQuery().eq(User::getEmail, newEmail)
                .ne(User::getId, userId).one() != null) {
            throw new BizException("该邮箱已被其他账号使用");
        }

        // 更新邮箱
        User update = new User();
        update.setId(userId);
        update.setEmail(newEmail);
        updateById(update);

        // 同步 session
        User sessionUser = (User) StpUtil.getSession().get("user");
        if (sessionUser != null) {
            sessionUser.setEmail(newEmail);
        }

        // 清理 Redis
        stringRedisTemplate.delete(RedisKeys.emailChangeCode(oldEmail));
        stringRedisTemplate.delete(RedisKeys.emailChangeNewCode(newEmail));
    }

    private void sendCodeToEmail(String email, String codeKey) {
        String rateKey = RedisKeys.emailRateLimit(email);
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(rateKey, "1", RedisKeys.EMAIL_RATE_LIMIT_TTL);
        if (Boolean.FALSE.equals(acquired)) {
            throw new BizException(RCode.TOO_MANY_REQUESTS.getCode(), "验证码发送过于频繁，请60秒后再试");
        }
        String code = emailCheckUtil.generateCode();
        stringRedisTemplate.opsForValue()
                .set(codeKey, code, RedisKeys.EMAIL_CODE_TTL);
        try {
            emailCheckUtil.sendCode(email, code);
        } catch (Exception e) {
            stringRedisTemplate.delete(codeKey);
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
        save(user);
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
        save(user);
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
        updateById(user);
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

    @Override
    public void updateUsername(Long userId, String newUsername) {
        if (!StringUtils.hasText(newUsername)) {
            throw new BizException("用户名不能为空");
        }
        if (lambdaQuery().eq(User::getUsername, newUsername)
                .ne(User::getId, userId).one() != null) {
            throw new BizException("用户名已存在");
        }
        User user = new User();
        user.setId(userId);
        user.setUsername(newUsername);
        updateById(user);
        // 同步更新 session 中的用户信息
        User sessionUser = (User) StpUtil.getSession().get("user");
        if (sessionUser != null) {
            sessionUser.setUsername(newUsername);
        }
    }

    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw new BizException("密码不能为空");
        }
        User user = getById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new BizException("原密码错误");
        }
        if (oldPassword.equals(newPassword)) {
            throw new BizException("新密码不能与旧密码相同");
        }
        User update = new User();
        update.setId(userId);
        update.setPassword(BCrypt.hashpw(newPassword));
        updateById(update);
    }

    @Override
    public String updateAvatar(Long userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BizException("文件不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BizException("只允许上传图片文件");
        }
        String originalName = file.getOriginalFilename();
        String ext = extractExt(originalName);
        if (!ALLOWED_IMAGE_EXTS.contains(ext.toLowerCase())) {
            throw new BizException("不支持的文件类型，仅允许 " + String.join(",", ALLOWED_IMAGE_EXTS));
        }

        File baseDir = resolveUploadDir();
        File avatarDir = new File(baseDir, "user_avatar");
        if (!avatarDir.exists()) {
            avatarDir.mkdirs();
        }

        String filename = UUID.randomUUID().toString() + "." + ext;
        Path targetPath = Paths.get(avatarDir.getAbsolutePath(), filename);
        try {
            Files.copy(file.getInputStream(), targetPath);
        } catch (IOException e) {
            throw new BizException("头像上传失败");
        }

        String url = "/uploads/user_avatar/" + filename;

        // 删除旧头像文件
        User oldUser = getById(userId);
        if (oldUser != null && oldUser.getAvatar() != null && !oldUser.getAvatar().isBlank()) {
            deleteAvatarFile(oldUser.getAvatar());
        }

        // 更新 DB
        User update = new User();
        update.setId(userId);
        update.setAvatar(url);
        updateById(update);

        // 同步 session
        User sessionUser = (User) StpUtil.getSession().get("user");
        if (sessionUser != null) {
            sessionUser.setAvatar(url);
        }

        return url;
    }

    private void deleteAvatarFile(String avatarUrl) {
        try {
            String filename = avatarUrl.substring(avatarUrl.lastIndexOf('/') + 1);
            Path filePath = Paths.get(resolveUploadDir().getAbsolutePath(), "user_avatar", filename);
            File file = filePath.toFile();
            if (file.exists() && file.isFile()) {
                file.delete();
            }
        } catch (Exception ignored) {
            // 删除失败不影响主流程
        }
    }

    private File resolveUploadDir() {
        File dir = new File(uploadPath);
        if (!dir.isAbsolute()) {
            dir = new File(System.getProperty("user.dir"), uploadPath);
        }
        return dir;
    }

    private static String extractExt(String filename) {
        if (filename == null || filename.isEmpty()) return "";
        int dot = filename.lastIndexOf('.');
        if (dot <= 0 || dot == filename.length() - 1) return "";
        return filename.substring(dot + 1);
    }

}
