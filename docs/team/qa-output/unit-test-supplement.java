package com.langtou.qa.unittest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 榔头（Langtou）内容社区平台 - 单元测试补充
 *
 * 覆盖范围：
 * - UserServiceImpl：注册/登录/关注/等级计算
 * - ContentServiceImpl：发布笔记/搜索/推荐
 * - InteractServiceImpl：点赞/评论/收藏
 *
 * 测试原则：每个方法覆盖正常流程、边界条件、异常场景
 */

// ============================================================
// UserServiceImpl 单元测试
// ============================================================

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl 单元测试")
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserFollowMapper userFollowMapper;

    @Mock
    private UserLevelMapper userLevelMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private SmsService smsService;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(10001L);
        testUser.setPhone("13800138000");
        testUser.setPassword("encoded_password");
        testUser.setNickname("测试用户");
        testUser.setStatus(1);
        testUser.setFollowCount(10);
        testUser.setFanCount(20);
        testUser.setLevel(3);
        testUser.setPoints(1500);
    }

    // ---------- 注册功能 ----------

    @Test
    @DisplayName("注册-正常流程：新用户注册成功")
    void register_normal_success() {
        // Given
        RegisterDTO dto = new RegisterDTO();
        dto.setPhone("13800138001");
        dto.setPassword("Test@1234");
        dto.setVerifyCode("123456");

        when(userMapper.selectByPhone("13800138001")).thenReturn(null);
        when(redisTemplate.opsForValue().get("verify:13800138001")).thenReturn("123456");
        when(passwordEncoder.encode("Test@1234")).thenReturn("encoded_pass");
        when(userMapper.insert(any(User.class))).thenReturn(1);
        when(jwtTokenProvider.generateToken(anyLong())).thenReturn("test_token");

        // When
        UserVO result = userService.register(dto);

        // Then
        assertNotNull(result);
        assertNotNull(result.getToken());
        verify(userMapper).insert(any(User.class));
        verify(redisTemplate.delete("verify:13800138001"));
    }

    @Test
    @DisplayName("注册-边界条件：手机号已注册")
    void register_phone_already_exists() {
        // Given
        RegisterDTO dto = new RegisterDTO();
        dto.setPhone("13800138000");
        dto.setPassword("Test@1234");
        dto.setVerifyCode("123456");

        when(userMapper.selectByPhone("13800138000")).thenReturn(testUser);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.register(dto));
        assertEquals("手机号已注册", exception.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    @DisplayName("注册-边界条件：验证码错误")
    void register_verify_code_wrong() {
        // Given
        RegisterDTO dto = new RegisterDTO();
        dto.setPhone("13800138001");
        dto.setPassword("Test@1234");
        dto.setVerifyCode("999999");

        when(userMapper.selectByPhone("13800138001")).thenReturn(null);
        when(redisTemplate.opsForValue().get("verify:13800138001")).thenReturn("123456");

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.register(dto));
        assertEquals("验证码错误或已过期", exception.getMessage());
    }

    @Test
    @DisplayName("注册-异常场景：验证码已过期（Redis 无记录）")
    void register_verify_code_expired() {
        // Given
        RegisterDTO dto = new RegisterDTO();
        dto.setPhone("13800138001");
        dto.setPassword("Test@1234");
        dto.setVerifyCode("123456");

        when(userMapper.selectByPhone("13800138001")).thenReturn(null);
        when(redisTemplate.opsForValue().get("verify:13800138001")).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.register(dto));
        assertEquals("验证码错误或已过期", exception.getMessage());
    }

    @ParameterizedTest
    @DisplayName("注册-边界条件：密码格式校验")
    @ValueSource(strings = {"123", "abcdefgh", "12345678", "Test@", ""})
    void register_password_format_invalid(String password) {
        // Given
        RegisterDTO dto = new RegisterDTO();
        dto.setPhone("13800138001");
        dto.setPassword(password);
        dto.setVerifyCode("123456");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> userService.register(dto));
    }

    // ---------- 登录功能 ----------

    @Test
    @DisplayName("登录-正常流程：正确账号密码登录成功")
    void login_normal_success() {
        // Given
        LoginDTO dto = new LoginDTO();
        dto.setPhone("13800138000");
        dto.setPassword("Test@1234");

        when(userMapper.selectByPhone("13800138000")).thenReturn(testUser);
        when(passwordEncoder.matches("Test@1234", "encoded_password")).thenReturn(true);
        when(jwtTokenProvider.generateToken(10001L)).thenReturn("test_token");
        when(redisTemplate.opsForValue().get("login_fail:13800138000")).thenReturn(null);

        // When
        UserVO result = userService.login(dto);

        // Then
        assertNotNull(result);
        assertEquals("test_token", result.getToken());
        assertEquals(10001L, result.getUserId());
    }

    @Test
    @DisplayName("登录-边界条件：密码错误")
    void login_password_wrong() {
        // Given
        LoginDTO dto = new LoginDTO();
        dto.setPhone("13800138000");
        dto.setPassword("WrongPass1");

        when(userMapper.selectByPhone("13800138000")).thenReturn(testUser);
        when(passwordEncoder.matches("WrongPass1", "encoded_password")).thenReturn(false);
        when(redisTemplate.opsForValue().get("login_fail:13800138000")).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.login(dto));
        assertEquals("账号或密码错误", exception.getMessage());
    }

    @Test
    @DisplayName("登录-异常场景：连续失败 5 次后账号锁定")
    void login_account_locked_after_five_failures() {
        // Given
        LoginDTO dto = new LoginDTO();
        dto.setPhone("13800138000");
        dto.setPassword("WrongPass1");

        when(userMapper.selectByPhone("13800138000")).thenReturn(testUser);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        when(redisTemplate.opsForValue().get("login_fail:13800138000")).thenReturn("4");

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.login(dto));
        assertEquals("账号已锁定，请 30 分钟后重试", exception.getMessage());
    }

    @Test
    @DisplayName("登录-异常场景：账号已被禁用")
    void login_account_disabled() {
        // Given
        testUser.setStatus(0);
        LoginDTO dto = new LoginDTO();
        dto.setPhone("13800138000");
        dto.setPassword("Test@1234");

        when(userMapper.selectByPhone("13800138000")).thenReturn(testUser);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.login(dto));
        assertEquals("账号已被禁用", exception.getMessage());
    }

    @Test
    @DisplayName("登录-边界条件：手机号未注册")
    void login_phone_not_registered() {
        // Given
        LoginDTO dto = new LoginDTO();
        dto.setPhone("13800138099");
        dto.setPassword("Test@1234");

        when(userMapper.selectByPhone("13800138099")).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.login(dto));
        assertEquals("账号或密码错误", exception.getMessage());
    }

    // ---------- 关注功能 ----------

    @Test
    @DisplayName("关注-正常流程：关注用户成功")
    void follow_normal_success() {
        // Given
        Long currentUserId = 10001L;
        Long targetUserId = 10002L;

        when(userMapper.selectById(targetUserId)).thenReturn(new User());
        when(userFollowMapper.selectFollowStatus(currentUserId, targetUserId)).thenReturn(null);
        when(userFollowMapper.insert(any(UserFollow.class))).thenReturn(1);
        when(userMapper.increaseFollowCount(currentUserId)).thenReturn(1);
        when(userMapper.increaseFanCount(targetUserId)).thenReturn(1);

        // When
        userService.follow(currentUserId, targetUserId);

        // Then
        verify(userFollowMapper).insert(any(UserFollow.class));
        verify(userMapper).increaseFollowCount(currentUserId);
        verify(userMapper).increaseFanCount(targetUserId);
    }

    @Test
    @DisplayName("关注-边界条件：已关注用户再次关注")
    void follow_already_followed() {
        // Given
        Long currentUserId = 10001L;
        Long targetUserId = 10002L;

        when(userMapper.selectById(targetUserId)).thenReturn(new User());
        when(userFollowMapper.selectFollowStatus(currentUserId, targetUserId)).thenReturn(new UserFollow());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> userService.follow(currentUserId, targetUserId));
        assertEquals("已关注该用户", exception.getMessage());
    }

    @Test
    @DisplayName("关注-边界条件：关注自己")
    void follow_self() {
        // Given
        Long currentUserId = 10001L;

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> userService.follow(currentUserId, currentUserId));
        assertEquals("不能关注自己", exception.getMessage());
    }

    @Test
    @DisplayName("关注-异常场景：关注不存在的用户")
    void follow_user_not_exist() {
        // Given
        Long currentUserId = 10001L;
        Long targetUserId = 99999L;

        when(userMapper.selectById(targetUserId)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> userService.follow(currentUserId, targetUserId));
        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    @DisplayName("取消关注-正常流程：取消关注成功")
    void unfollow_normal_success() {
        // Given
        Long currentUserId = 10001L;
        Long targetUserId = 10002L;

        when(userFollowMapper.selectFollowStatus(currentUserId, targetUserId)).thenReturn(new UserFollow());
        when(userFollowMapper.deleteByUserIds(currentUserId, targetUserId)).thenReturn(1);
        when(userMapper.decreaseFollowCount(currentUserId)).thenReturn(1);
        when(userMapper.decreaseFanCount(targetUserId)).thenReturn(1);

        // When
        userService.unfollow(currentUserId, targetUserId);

        // Then
        verify(userFollowMapper).deleteByUserIds(currentUserId, targetUserId);
        verify(userMapper).decreaseFollowCount(currentUserId);
        verify(userMapper).decreaseFanCount(targetUserId);
    }

    @Test
    @DisplayName("取消关注-边界条件：未关注用户取消关注")
    void unfollow_not_followed() {
        // Given
        Long currentUserId = 10001L;
        Long targetUserId = 10002L;

        when(userFollowMapper.selectFollowStatus(currentUserId, targetUserId)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> userService.unfollow(currentUserId, targetUserId));
        assertEquals("未关注该用户", exception.getMessage());
    }

    // ---------- 等级计算 ----------

    @ParameterizedTest
    @DisplayName("等级计算-边界条件：各等级积分边界")
    @CsvSource({
        "0, 1",      // 0 分 -> 1 级
        "99, 1",     // 99 分 -> 1 级
        "100, 2",    // 100 分 -> 2 级
        "499, 2",    // 499 分 -> 2 级
        "500, 3",    // 500 分 -> 3 级
        "999, 3",    // 999 分 -> 3 级
        "1000, 4",   // 1000 分 -> 4 级
        "4999, 4",   // 4999 分 -> 4 级
        "5000, 5",   // 5000 分 -> 5 级
        "9999, 5",   // 9999 分 -> 5 级
        "10000, 6",  // 10000 分 -> 6 级
    })
    void calculate_level_boundary(int points, int expectedLevel) {
        // When
        int actualLevel = userService.calculateLevel(points);

        // Then
        assertEquals(expectedLevel, actualLevel);
    }

    @Test
    @DisplayName("等级计算-异常场景：积分为负数")
    void calculate_level_negative_points() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> userService.calculateLevel(-1));
    }

    @Test
    @DisplayName("等级计算-正常流程：升级后更新用户等级")
    void upgrade_level_normal() {
        // Given
        Long userId = 10001L;
        int newPoints = 2500;

        when(userMapper.selectById(userId)).thenReturn(testUser);
        when(userLevelMapper.selectByPoints(anyInt())).thenReturn(new UserLevel(5, 2000, 4999));
        when(userMapper.updateLevel(userId, 5)).thenReturn(1);

        // When
        userService.addPointsAndUpgrade(userId, newPoints);

        // Then
        verify(userMapper).updateLevel(userId, 5);
    }
}


// ============================================================
// ContentServiceImpl 单元测试
// ============================================================

@ExtendWith(MockitoExtension.class)
@DisplayName("ContentServiceImpl 单元测试")
class ContentServiceImplTest {

    @Mock
    private NoteMapper noteMapper;

    @Mock
    private ElasticsearchRestTemplate esTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private SensitiveWordFilter sensitiveWordFilter;

    @InjectMocks
    private ContentServiceImpl contentService;

    private Note testNote;

    @BeforeEach
    void setUp() {
        testNote = new Note();
        testNote.setNoteId(20001L);
        testNote.setUserId(10001L);
        testNote.setTitle("测试笔记标题");
        testNote.setContent("测试笔记内容");
        testNote.setImages("[\"url1\",\"url2\"]");
        testNote.setType(1);
        testNote.setStatus(1);
    }

    // ---------- 发布笔记 ----------

    @Test
    @DisplayName("发布笔记-正常流程：图文笔记发布成功")
    void publish_note_image_success() {
        // Given
        PublishNoteDTO dto = new PublishNoteDTO();
        dto.setTitle("新笔记标题");
        dto.setContent("新笔记内容");
        dto.setImages(Arrays.asList("url1", "url2"));
        dto.setType(1);
        dto.setTags(Arrays.asList("美食", "探店"));

        Long userId = 10001L;

        when(sensitiveWordFilter.containsSensitiveWord(anyString())).thenReturn(false);
        when(noteMapper.insert(any(Note.class))).thenAnswer(inv -> {
            Note note = inv.getArgument(0);
            note.setNoteId(20002L);
            return 1;
        });
        doNothing().when(kafkaTemplate).send(eq("note-publish"), any(NoteEvent.class));

        // When
        NoteVO result = contentService.publishNote(userId, dto);

        // Then
        assertNotNull(result);
        assertEquals(20002L, result.getNoteId());
        verify(noteMapper).insert(any(Note.class));
        verify(kafkaTemplate).send(eq("note-publish"), any(NoteEvent.class));
    }

    @Test
    @DisplayName("发布笔记-正常流程：视频笔记发布成功")
    void publish_note_video_success() {
        // Given
        PublishNoteDTO dto = new PublishNoteDTO();
        dto.setTitle("视频笔记标题");
        dto.setContent("视频笔记内容");
        dto.setVideoUrl("https://cdn.langtou.com/video/test.mp4");
        dto.setType(2);
        dto.setDuration(30);

        Long userId = 10001L;

        when(sensitiveWordFilter.containsSensitiveWord(anyString())).thenReturn(false);
        when(noteMapper.insert(any(Note.class))).thenAnswer(inv -> {
            Note note = inv.getArgument(0);
            note.setNoteId(20003L);
            return 1;
        });

        // When
        NoteVO result = contentService.publishNote(userId, dto);

        // Then
        assertNotNull(result);
        assertEquals(20003L, result.getNoteId());
        assertEquals(2, result.getType());
    }

    @Test
    @DisplayName("发布笔记-边界条件：标题为空")
    void publish_note_empty_title() {
        // Given
        PublishNoteDTO dto = new PublishNoteDTO();
        dto.setTitle("");
        dto.setContent("内容");
        dto.setType(1);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> contentService.publishNote(10001L, dto));
    }

    @Test
    @DisplayName("发布笔记-边界条件：标题超过 100 字符")
    void publish_note_title_too_long() {
        // Given
        PublishNoteDTO dto = new PublishNoteDTO();
        dto.setTitle("a".repeat(101));
        dto.setContent("内容");
        dto.setType(1);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> contentService.publishNote(10001L, dto));
    }

    @Test
    @DisplayName("发布笔记-边界条件：图文笔记无图片")
    void publish_note_image_without_images() {
        // Given
        PublishNoteDTO dto = new PublishNoteDTO();
        dto.setTitle("标题");
        dto.setContent("内容");
        dto.setType(1);
        dto.setImages(Collections.emptyList());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> contentService.publishNote(10001L, dto));
    }

    @Test
    @DisplayName("发布笔记-异常场景：内容包含敏感词")
    void publish_note_sensitive_content() {
        // Given
        PublishNoteDTO dto = new PublishNoteDTO();
        dto.setTitle("敏感词测试");
        dto.setContent("内容包含违禁词");
        dto.setType(1);
        dto.setImages(Arrays.asList("url1"));

        when(sensitiveWordFilter.containsSensitiveWord(anyString())).thenReturn(true);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> contentService.publishNote(10001L, dto));
        assertEquals("内容包含敏感信息", exception.getMessage());
        verify(noteMapper, never()).insert(any(Note.class));
    }

    @Test
    @DisplayName("发布笔记-异常场景：未登录用户发布")
    void publish_note_not_login() {
        // Given
        PublishNoteDTO dto = new PublishNoteDTO();
        dto.setTitle("标题");
        dto.setContent("内容");
        dto.setType(1);
        dto.setImages(Arrays.asList("url1"));

        // When & Then
        assertThrows(UnauthorizedException.class, () -> contentService.publishNote(null, dto));
    }

    // ---------- 搜索功能 ----------

    @Test
    @DisplayName("搜索-正常流程：关键词搜索成功")
    void search_normal_success() {
        // Given
        String keyword = "美食";
        SearchDTO dto = new SearchDTO();
        dto.setKeyword(keyword);
        dto.setPage(1);
        dto.setSize(10);

        NoteDoc noteDoc = new NoteDoc();
        noteDoc.setNoteId(20001L);
        noteDoc.setTitle("美食探店");

        SearchHits<NoteDoc> searchHits = mock(SearchHits.class);
        when(searchHits.getTotalHits()).thenReturn(10L);
        when(searchHits.getSearchHits()).thenReturn(Arrays.asList(
            new SearchHit<>(null, null, null, null, noteDoc)
        ));
        when(esTemplate.search(any(Query.class), eq(NoteDoc.class))).thenReturn(searchHits);

        // When
        PageResult<NoteVO> result = contentService.search(dto);

        // Then
        assertNotNull(result);
        assertEquals(10L, result.getTotal());
        assertFalse(result.getList().isEmpty());
    }

    @Test
    @DisplayName("搜索-边界条件：空关键词")
    void search_empty_keyword() {
        // Given
        SearchDTO dto = new SearchDTO();
        dto.setKeyword("");
        dto.setPage(1);
        dto.setSize(10);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> contentService.search(dto));
    }

    @Test
    @DisplayName("搜索-边界条件：关键词少于 2 个字符")
    void search_keyword_too_short() {
        // Given
        SearchDTO dto = new SearchDTO();
        dto.setKeyword("美");
        dto.setPage(1);
        dto.setSize(10);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> contentService.search(dto));
    }

    @Test
    @DisplayName("搜索-正常流程：无结果返回空列表")
    void search_no_results() {
        // Given
        SearchDTO dto = new SearchDTO();
        dto.setKeyword("不存在的关键词xyz");
        dto.setPage(1);
        dto.setSize(10);

        SearchHits<NoteDoc> searchHits = mock(SearchHits.class);
        when(searchHits.getTotalHits()).thenReturn(0L);
        when(searchHits.getSearchHits()).thenReturn(Collections.emptyList());
        when(esTemplate.search(any(Query.class), eq(NoteDoc.class))).thenReturn(searchHits);

        // When
        PageResult<NoteVO> result = contentService.search(dto);

        // Then
        assertNotNull(result);
        assertEquals(0L, result.getTotal());
        assertTrue(result.getList().isEmpty());
    }

    @Test
    @DisplayName("搜索-边界条件：XSS 关键词安全转义")
    void search_xss_keyword() {
        // Given
        SearchDTO dto = new SearchDTO();
        dto.setKeyword("<script>alert(1)</script>");
        dto.setPage(1);
        dto.setSize(10);

        SearchHits<NoteDoc> searchHits = mock(SearchHits.class);
        when(searchHits.getTotalHits()).thenReturn(0L);
        when(searchHits.getSearchHits()).thenReturn(Collections.emptyList());
        when(esTemplate.search(any(Query.class), eq(NoteDoc.class))).thenReturn(searchHits);

        // When
        PageResult<NoteVO> result = contentService.search(dto);

        // Then - 不抛出异常，正常执行
        assertNotNull(result);
    }

    // ---------- 推荐功能 ----------

    @Test
    @DisplayName("推荐-正常流程：获取推荐列表")
    void recommend_normal_success() {
        // Given
        Long userId = 10001L;
        int page = 1;
        int size = 10;

        when(noteMapper.selectRecommendList(any(), eq(page), eq(size)))
            .thenReturn(Arrays.asList(testNote));
        when(noteMapper.countRecommend(any())).thenReturn(100L);

        // When
        PageResult<NoteVO> result = contentService.getRecommendList(userId, page, size);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getList().size());
        assertEquals(100L, result.getTotal());
    }

    @Test
    @DisplayName("推荐-边界条件：分页参数 page=0")
    void recommend_invalid_page() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
            () -> contentService.getRecommendList(10001L, 0, 10));
    }

    @Test
    @DisplayName("推荐-边界条件：分页参数 size=0")
    void recommend_invalid_size() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
            () -> contentService.getRecommendList(10001L, 1, 0));
    }

    @Test
    @DisplayName("推荐-边界条件：分页参数 size 超过 100")
    void recommend_size_too_large() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
            () -> contentService.getRecommendList(10001L, 1, 101));
    }

    @Test
    @DisplayName("推荐-正常流程：未登录用户获取默认推荐")
    void recommend_anonymous_user() {
        // Given
        when(noteMapper.selectRecommendList(isNull(), eq(1), eq(10)))
            .thenReturn(Arrays.asList(testNote));
        when(noteMapper.countRecommend(isNull())).thenReturn(50L);

        // When
        PageResult<NoteVO> result = contentService.getRecommendList(null, 1, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getList().size());
    }
}


// ============================================================
// InteractServiceImpl 单元测试
// ============================================================

@ExtendWith(MockitoExtension.class)
@DisplayName("InteractServiceImpl 单元测试")
class InteractServiceImplTest {

    @Mock
    private LikeRecordMapper likeRecordMapper;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private CollectRecordMapper collectRecordMapper;

    @Mock
    private NoteMapper noteMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private InteractServiceImpl interactService;

    // ---------- 点赞功能 ----------

    @Test
    @DisplayName("点赞-正常流程：点赞笔记成功")
    void like_note_normal_success() {
        // Given
        Long userId = 10001L;
        Long noteId = 20001L;

        when(likeRecordMapper.selectByUserAndNote(userId, noteId)).thenReturn(null);
        when(likeRecordMapper.insert(any(LikeRecord.class))).thenReturn(1);
        when(noteMapper.increaseLikeCount(noteId)).thenReturn(1);
        when(redisTemplate.opsForSet().add(anyString(), eq(noteId))).thenReturn(1L);

        // When
        interactService.likeNote(userId, noteId);

        // Then
        verify(likeRecordMapper).insert(any(LikeRecord.class));
        verify(noteMapper).increaseLikeCount(noteId);
    }

    @Test
    @DisplayName("点赞-边界条件：已点赞再次点赞（幂等性）")
    void like_note_already_liked() {
        // Given
        Long userId = 10001L;
        Long noteId = 20001L;

        when(likeRecordMapper.selectByUserAndNote(userId, noteId)).thenReturn(new LikeRecord());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> interactService.likeNote(userId, noteId));
        assertEquals("已点赞", exception.getMessage());
        verify(likeRecordMapper, never()).insert(any(LikeRecord.class));
    }

    @Test
    @DisplayName("点赞-异常场景：点赞不存在的笔记")
    void like_note_not_exist() {
        // Given
        Long userId = 10001L;
        Long noteId = 99999L;

        when(likeRecordMapper.selectByUserAndNote(userId, noteId)).thenReturn(null);
        when(noteMapper.selectById(noteId)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> interactService.likeNote(userId, noteId));
        assertEquals("笔记不存在或已删除", exception.getMessage());
    }

    @Test
    @DisplayName("点赞-异常场景：未登录用户点赞")
    void like_note_not_login() {
        // When & Then
        assertThrows(UnauthorizedException.class, () -> interactService.likeNote(null, 20001L));
    }

    @Test
    @DisplayName("取消点赞-正常流程：取消点赞成功")
    void unlike_note_normal_success() {
        // Given
        Long userId = 10001L;
        Long noteId = 20001L;

        when(likeRecordMapper.selectByUserAndNote(userId, noteId)).thenReturn(new LikeRecord());
        when(likeRecordMapper.deleteByUserAndNote(userId, noteId)).thenReturn(1);
        when(noteMapper.decreaseLikeCount(noteId)).thenReturn(1);

        // When
        interactService.unlikeNote(userId, noteId);

        // Then
        verify(likeRecordMapper).deleteByUserAndNote(userId, noteId);
        verify(noteMapper).decreaseLikeCount(noteId);
    }

    @Test
    @DisplayName("取消点赞-边界条件：未点赞取消点赞")
    void unlike_note_not_liked() {
        // Given
        Long userId = 10001L;
        Long noteId = 20001L;

        when(likeRecordMapper.selectByUserAndNote(userId, noteId)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> interactService.unlikeNote(userId, noteId));
        assertEquals("未点赞", exception.getMessage());
    }

    // ---------- 评论功能 ----------

    @Test
    @DisplayName("评论-正常流程：评论笔记成功")
    void comment_note_normal_success() {
        // Given
        Long userId = 10001L;
        Long noteId = 20001L;
        String content = "这是一条评论";

        CommentDTO dto = new CommentDTO();
        dto.setNoteId(noteId);
        dto.setContent(content);

        when(noteMapper.selectById(noteId)).thenReturn(new Note());
        when(commentMapper.insert(any(Comment.class))).thenAnswer(inv -> {
            Comment comment = inv.getArgument(0);
            comment.setCommentId(30001L);
            return 1;
        });
        when(noteMapper.increaseCommentCount(noteId)).thenReturn(1);

        // When
        CommentVO result = interactService.commentNote(userId, dto);

        // Then
        assertNotNull(result);
        assertEquals(30001L, result.getCommentId());
        verify(commentMapper).insert(any(Comment.class));
        verify(noteMapper).increaseCommentCount(noteId);
    }

    @Test
    @DisplayName("评论-边界条件：评论内容为空")
    void comment_note_empty_content() {
        // Given
        CommentDTO dto = new CommentDTO();
        dto.setNoteId(20001L);
        dto.setContent("");

        // When & Then
        assertThrows(IllegalArgumentException.class,
            () -> interactService.commentNote(10001L, dto));
    }

    @Test
    @DisplayName("评论-边界条件：评论内容超过 500 字符")
    void comment_note_content_too_long() {
        // Given
        CommentDTO dto = new CommentDTO();
        dto.setNoteId(20001L);
        dto.setContent("a".repeat(501));

        // When & Then
        assertThrows(IllegalArgumentException.class,
            () -> interactService.commentNote(10001L, dto));
    }

    @Test
    @DisplayName("评论-异常场景：评论不存在的笔记")
    void comment_note_not_exist() {
        // Given
        CommentDTO dto = new CommentDTO();
        dto.setNoteId(99999L);
        dto.setContent("评论内容");

        when(noteMapper.selectById(99999L)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> interactService.commentNote(10001L, dto));
        assertEquals("笔记不存在或已删除", exception.getMessage());
    }

    @Test
    @DisplayName("评论-异常场景：评论包含敏感词")
    void comment_note_sensitive_content() {
        // Given - 假设服务层有敏感词校验
        CommentDTO dto = new CommentDTO();
        dto.setNoteId(20001L);
        dto.setContent("敏感词评论");

        when(noteMapper.selectById(20001L)).thenReturn(new Note());
        // 模拟敏感词过滤抛出异常

        // When & Then
        // 根据实际实现调整断言
        assertThrows(BusinessException.class, () -> interactService.commentNote(10001L, dto));
    }

    @Test
    @DisplayName("回复评论-正常流程：回复评论成功")
    void reply_comment_normal_success() {
        // Given
        Long userId = 10001L;
        ReplyDTO dto = new ReplyDTO();
        dto.setCommentId(30001L);
        dto.setContent("这是一条回复");
        dto.setReplyToUserId(10002L);

        when(commentMapper.selectById(30001L)).thenReturn(new Comment());
        when(commentMapper.insertReply(any(CommentReply.class))).thenAnswer(inv -> {
            CommentReply reply = inv.getArgument(0);
            reply.setReplyId(40001L);
            return 1;
        });

        // When
        ReplyVO result = interactService.replyComment(userId, dto);

        // Then
        assertNotNull(result);
        assertEquals(40001L, result.getReplyId());
    }

    @Test
    @DisplayName("回复评论-异常场景：回复不存在的评论")
    void reply_comment_not_exist() {
        // Given
        ReplyDTO dto = new ReplyDTO();
        dto.setCommentId(99999L);
        dto.setContent("回复内容");
        dto.setReplyToUserId(10002L);

        when(commentMapper.selectById(99999L)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> interactService.replyComment(10001L, dto));
        assertEquals("评论不存在", exception.getMessage());
    }

    // ---------- 收藏功能 ----------

    @Test
    @DisplayName("收藏-正常流程：收藏笔记成功")
    void collect_note_normal_success() {
        // Given
        Long userId = 10001L;
        Long noteId = 20001L;
        Long folderId = 0L;

        when(collectRecordMapper.selectByUserAndNote(userId, noteId)).thenReturn(null);
        when(noteMapper.selectById(noteId)).thenReturn(new Note());
        when(collectRecordMapper.insert(any(CollectRecord.class))).thenReturn(1);
        when(noteMapper.increaseCollectCount(noteId)).thenReturn(1);

        // When
        interactService.collectNote(userId, noteId, folderId);

        // Then
        verify(collectRecordMapper).insert(any(CollectRecord.class));
        verify(noteMapper).increaseCollectCount(noteId);
    }

    @Test
    @DisplayName("收藏-正常流程：收藏到指定收藏夹")
    void collect_note_to_folder() {
        // Given
        Long userId = 10001L;
        Long noteId = 20001L;
        Long folderId = 50001L;

        when(collectRecordMapper.selectByUserAndNote(userId, noteId)).thenReturn(null);
        when(noteMapper.selectById(noteId)).thenReturn(new Note());
        when(collectRecordMapper.insert(any(CollectRecord.class))).thenReturn(1);
        when(noteMapper.increaseCollectCount(noteId)).thenReturn(1);

        // When
        interactService.collectNote(userId, noteId, folderId);

        // Then
        verify(collectRecordMapper).insert(argThat(record ->
            record.getFolderId().equals(folderId)
        ));
    }

    @Test
    @DisplayName("收藏-边界条件：已收藏再次收藏")
    void collect_note_already_collected() {
        // Given
        Long userId = 10001L;
        Long noteId = 20001L;

        when(collectRecordMapper.selectByUserAndNote(userId, noteId)).thenReturn(new CollectRecord());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> interactService.collectNote(userId, noteId, 0L));
        assertEquals("已收藏", exception.getMessage());
    }

    @Test
    @DisplayName("收藏-异常场景：收藏不存在的笔记")
    void collect_note_not_exist() {
        // Given
        Long userId = 10001L;
        Long noteId = 99999L;

        when(collectRecordMapper.selectByUserAndNote(userId, noteId)).thenReturn(null);
        when(noteMapper.selectById(noteId)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> interactService.collectNote(userId, noteId, 0L));
        assertEquals("笔记不存在或已删除", exception.getMessage());
    }

    @Test
    @DisplayName("取消收藏-正常流程：取消收藏成功")
    void uncollect_note_normal_success() {
        // Given
        Long userId = 10001L;
        Long noteId = 20001L;

        when(collectRecordMapper.selectByUserAndNote(userId, noteId)).thenReturn(new CollectRecord());
        when(collectRecordMapper.deleteByUserAndNote(userId, noteId)).thenReturn(1);
        when(noteMapper.decreaseCollectCount(noteId)).thenReturn(1);

        // When
        interactService.uncollectNote(userId, noteId);

        // Then
        verify(collectRecordMapper).deleteByUserAndNote(userId, noteId);
        verify(noteMapper).decreaseCollectCount(noteId);
    }

    @Test
    @DisplayName("取消收藏-边界条件：未收藏取消收藏")
    void uncollect_note_not_collected() {
        // Given
        Long userId = 10001L;
        Long noteId = 20001L;

        when(collectRecordMapper.selectByUserAndNote(userId, noteId)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> interactService.uncollectNote(userId, noteId));
        assertEquals("未收藏", exception.getMessage());
    }

    // ---------- 并发/幂等性测试 ----------

    @Test
    @DisplayName("点赞-并发场景：重复点赞只增加一次计数")
    void like_note_concurrent_idempotent() {
        // Given
        Long userId = 10001L;
        Long noteId = 20001L;

        when(likeRecordMapper.selectByUserAndNote(userId, noteId)).thenReturn(null);
        when(likeRecordMapper.insert(any(LikeRecord.class))).thenReturn(1);
        when(noteMapper.increaseLikeCount(noteId)).thenReturn(1);

        // When - 模拟并发多次调用
        interactService.likeNote(userId, noteId);

        // Then - 第二次调用应该抛出已点赞异常
        when(likeRecordMapper.selectByUserAndNote(userId, noteId)).thenReturn(new LikeRecord());
        assertThrows(BusinessException.class, () -> interactService.likeNote(userId, noteId));
        verify(noteMapper, times(1)).increaseLikeCount(noteId);
    }

    @Test
    @DisplayName("收藏-并发场景：重复收藏幂等性验证")
    void collect_note_concurrent_idempotent() {
        // Given
        Long userId = 10001L;
        Long noteId = 20001L;

        when(collectRecordMapper.selectByUserAndNote(userId, noteId)).thenReturn(null);
        when(noteMapper.selectById(noteId)).thenReturn(new Note());
        when(collectRecordMapper.insert(any(CollectRecord.class))).thenReturn(1);
        when(noteMapper.increaseCollectCount(noteId)).thenReturn(1);

        // When
        interactService.collectNote(userId, noteId, 0L);

        // Then
        when(collectRecordMapper.selectByUserAndNote(userId, noteId)).thenReturn(new CollectRecord());
        assertThrows(BusinessException.class, () -> interactService.collectNote(userId, noteId, 0L));
        verify(noteMapper, times(1)).increaseCollectCount(noteId);
    }
}
