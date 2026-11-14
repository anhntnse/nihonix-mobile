package com.nihonix.mobile

import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.auth.domain.model.ResetCodeVerification
import com.nihonix.mobile.feature.auth.domain.model.User
import com.nihonix.mobile.feature.auth.domain.repository.AuthRepository
import com.nihonix.mobile.feature.topics.domain.model.JlptLevel
import com.nihonix.mobile.feature.topics.domain.model.LocalizedText
import com.nihonix.mobile.feature.topics.domain.model.Topic
import com.nihonix.mobile.feature.topics.domain.model.UserLevel
import com.nihonix.mobile.feature.topics.domain.repository.TopicsLevelsRepository
import com.nihonix.mobile.feature.topics.presentation.topic_detail.TopicDetailViewModel
import com.nihonix.mobile.feature.topics.presentation.topics_list.TopicsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TopicsLevelsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeTopicsRepository: FakeTopicsLevelsRepository
    private lateinit var fakeAuthRepository: FakeAuthStatusRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeTopicsRepository = FakeTopicsLevelsRepository()
        fakeAuthRepository = FakeAuthStatusRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun topicsViewModel_loggedInUserLevel_loadsLevelsAndFilteredTopics() = runTest(testDispatcher) {
        fakeAuthRepository.loggedIn = true
        fakeTopicsRepository.userLevel = UserLevel(
            userId = "u-1",
            levelCode = "N5",
            level = fakeTopicsRepository.n5
        )

        val viewModel = TopicsViewModel(fakeTopicsRepository, fakeAuthRepository)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.levels.size)
        assertEquals("N5", state.userLevelCode)
        assertEquals("N5", state.selectedLevelCode)
        assertEquals("Beginner", state.selectedLevelDetail?.name?.en)
        assertEquals(2, state.topics.size)
        assertTrue(state.topics.all { it.levelCode == "N5" })
        assertFalse(state.isTopicsLoading)
        assertFalse(state.isTopicsEmpty)
        assertNull(state.topicsError)
    }

    @Test
    fun topicsViewModel_selectLevel_filtersTopicsAndAllShowsAll() = runTest(testDispatcher) {
        fakeAuthRepository.loggedIn = false
        val viewModel = TopicsViewModel(fakeTopicsRepository, fakeAuthRepository)
        testScheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedLevelCode)
        assertEquals(4, viewModel.uiState.value.topics.size)

        viewModel.selectLevel("N4")
        testScheduler.advanceUntilIdle()

        assertEquals("N4", viewModel.uiState.value.selectedLevelCode)
        assertEquals("N4", viewModel.uiState.value.selectedLevelDetail?.code)
        assertEquals(1, viewModel.uiState.value.topics.size)
        assertEquals("entertainment", viewModel.uiState.value.topics.first().slug)

        viewModel.selectLevel(null)
        testScheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedLevelCode)
        assertEquals(4, viewModel.uiState.value.topics.size)
    }

    @Test
    fun topicsViewModel_emptyTopics_setsEmptyState() = runTest(testDispatcher) {
        fakeAuthRepository.loggedIn = false
        fakeTopicsRepository.topicsByLevel["N3"] = emptyList()

        val viewModel = TopicsViewModel(fakeTopicsRepository, fakeAuthRepository)
        testScheduler.advanceUntilIdle()

        viewModel.selectLevel("N3")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isTopicsEmpty)
        assertTrue(state.topics.isEmpty())
        assertNull(state.topicsError)
        assertFalse(state.isTopicsLoading)
    }

    @Test
    fun topicsViewModel_topicsError_setsErrorState() = runTest(testDispatcher) {
        fakeAuthRepository.loggedIn = false
        fakeTopicsRepository.shouldFailTopics = true
        fakeTopicsRepository.errorMessage = "Network error"

        val viewModel = TopicsViewModel(fakeTopicsRepository, fakeAuthRepository)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Network error", state.topicsError)
        assertFalse(state.isTopicsLoading)
    }

    @Test
    fun topicsViewModel_updateMyLevel_updatesUserLevelAndReloadsTopics() = runTest(testDispatcher) {
        fakeAuthRepository.loggedIn = true
        fakeTopicsRepository.userLevel = UserLevel("u-1", "N5", fakeTopicsRepository.n5)

        val viewModel = TopicsViewModel(fakeTopicsRepository, fakeAuthRepository)
        testScheduler.advanceUntilIdle()

        viewModel.updateMyLevel("N3")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("N3", state.userLevelCode)
        assertEquals("N3", state.selectedLevelCode)
        assertNotNull(state.levelUpdateMessage)
        assertNull(state.levelUpdateError)
        assertEquals(1, state.topics.size)
        assertEquals("economy", state.topics.first().slug)
    }

    @Test
    fun topicsViewModel_searchQuery_filtersTopicsAcrossLanguagesAndSlug() = runTest(testDispatcher) {
        fakeAuthRepository.loggedIn = false
        val viewModel = TopicsViewModel(fakeTopicsRepository, fakeAuthRepository)
        testScheduler.advanceUntilIdle()

        viewModel.onSearchQueryChanged("Lời chào")
        assertEquals(1, viewModel.uiState.value.filteredTopics.size)
        assertEquals("greetings", viewModel.uiState.value.filteredTopics.first().slug)

        viewModel.onSearchQueryChanged("数字")
        assertEquals(1, viewModel.uiState.value.filteredTopics.size)
        assertEquals("numbers", viewModel.uiState.value.filteredTopics.first().slug)

        viewModel.onSearchQueryChanged("economy")
        assertEquals(1, viewModel.uiState.value.filteredTopics.size)
        assertEquals("N3", viewModel.uiState.value.filteredTopics.first().levelCode)
    }

    @Test
    fun topicDetailViewModel_success_loadsTopicAndLevel() = runTest(testDispatcher) {
        val viewModel = TopicDetailViewModel(fakeTopicsRepository)
        viewModel.loadTopicDetail("greetings")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isEmpty)
        assertNull(state.errorMessage)
        assertEquals("greetings", state.topic?.slug)
        assertEquals("N5", state.levelDetail?.code)
    }

    @Test
    fun topicDetailViewModel_withoutEmbeddedLevel_fetchesFallbackLevel() = runTest(testDispatcher) {
        val viewModel = TopicDetailViewModel(fakeTopicsRepository)
        viewModel.loadTopicDetail("entertainment")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("entertainment", state.topic?.slug)
        assertEquals("N4", state.levelDetail?.code)
        assertEquals("Elementary", state.levelDetail?.name?.en)
    }

    @Test
    fun topicDetailViewModel_error_setsErrorMessage() = runTest(testDispatcher) {
        fakeTopicsRepository.shouldFailTopicDetail = true
        fakeTopicsRepository.errorMessage = "Topic not found"

        val viewModel = TopicDetailViewModel(fakeTopicsRepository)
        viewModel.loadTopicDetail("missing-topic")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.topic)
        assertEquals("Topic not found", state.errorMessage)
    }

    @Test
    fun topicDetailViewModel_empty_setsEmptyState() = runTest(testDispatcher) {
        fakeTopicsRepository.returnEmptyTopicDetail = true

        val viewModel = TopicDetailViewModel(fakeTopicsRepository)
        viewModel.loadTopicDetail("empty-topic")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isEmpty)
        assertNull(state.topic)
    }

    private class FakeAuthStatusRepository : AuthRepository {
        var loggedIn: Boolean = false
        private val _currentUser = MutableStateFlow<User?>(null)
        override val currentUserFlow: StateFlow<User?> = _currentUser

        override fun isLoggedIn(): Boolean = loggedIn
        override fun login(username: String, password: String): Flow<Resource<User>> = flow {}
        override fun register(
            username: String,
            email: String,
            password: String,
            fullName: String?
        ): Flow<Resource<User>> = flow {}
        override fun logout(): Flow<Resource<Unit>> = flow {}
        override fun getCurrentUser(): Flow<Resource<User>> = flow {}
        override fun forgotPassword(email: String): Flow<Resource<String>> = flow {}
        override fun verifyResetCode(email: String, code: String): Flow<Resource<ResetCodeVerification>> = flow {}
        override fun resetPassword(token: String, password: String): Flow<Resource<String>> = flow {}
        override fun resendCode(email: String): Flow<Resource<String>> = flow {}
    }

    private class FakeTopicsLevelsRepository : TopicsLevelsRepository {
        val n5 = JlptLevel("N5", LocalizedText("Sơ cấp", "Beginner", "初級"))
        val n4 = JlptLevel("N4", LocalizedText("Sơ trung cấp", "Elementary", "初級中級"))
        val n3 = JlptLevel("N3", LocalizedText("Trung cấp", "Intermediate", "中級"))

        val allLevels = listOf(n5, n4, n3)

        val greetingsTopic = Topic(
            id = "t-1",
            slug = "greetings",
            levelCode = "N5",
            title = LocalizedText("Lời chào hỏi", "Greetings", "挨拶"),
            iconType = "material",
            iconCode = 59389,
            order = 1,
            level = n5
        )
        val numbersTopic = Topic(
            id = "t-2",
            slug = "numbers",
            levelCode = "N5",
            title = LocalizedText("Số đếm", "Numbers", "数字"),
            iconType = "material",
            iconCode = 58835,
            order = 2,
            level = n5
        )
        val entertainmentTopic = Topic(
            id = "t-3",
            slug = "entertainment",
            levelCode = "N4",
            title = LocalizedText("Giải trí", "Entertainment", "娯楽"),
            iconType = "material",
            iconCode = 58286,
            order = 2,
            level = null // tests fallback getLevelByCode
        )
        val economyTopic = Topic(
            id = "t-4",
            slug = "economy",
            levelCode = "N3",
            title = LocalizedText("Kinh tế", "Economy", "経済"),
            iconType = "material",
            iconCode = 59817,
            order = 2,
            level = n3
        )

        val topicsByLevel: MutableMap<String, List<Topic>> = mutableMapOf(
            "N5" to listOf(greetingsTopic, numbersTopic),
            "N4" to listOf(entertainmentTopic),
            "N3" to listOf(economyTopic)
        )

        var userLevel: UserLevel = UserLevel("u-1", null, null)
        var shouldFailTopics: Boolean = false
        var shouldFailTopicDetail: Boolean = false
        var returnEmptyTopicDetail: Boolean = false
        var errorMessage: String = "Error occurred"

        override fun getLevels(): Flow<Resource<List<JlptLevel>>> = flow {
            emit(Resource.Loading)
            emit(Resource.Success(allLevels))
        }

        override fun getLevelByCode(code: String): Flow<Resource<JlptLevel>> = flow {
            emit(Resource.Loading)
            val found = allLevels.find { it.code.equals(code, ignoreCase = true) }
            if (found != null) {
                emit(Resource.Success(found))
            } else {
                emit(Resource.Error("Level $code not found", 404))
            }
        }

        override fun getUserLevel(): Flow<Resource<UserLevel>> = flow {
            emit(Resource.Loading)
            emit(Resource.Success(userLevel))
        }

        override fun updateUserLevel(levelCode: String): Flow<Resource<UserLevel>> = flow {
            emit(Resource.Loading)
            val matched = allLevels.find { it.code.equals(levelCode, ignoreCase = true) }
            userLevel = UserLevel("u-1", levelCode.uppercase(), matched)
            emit(Resource.Success(userLevel, "Level updated to ${levelCode.uppercase()}"))
        }

        override fun getTopics(levelCode: String?): Flow<Resource<List<Topic>>> = flow {
            emit(Resource.Loading)
            if (shouldFailTopics) {
                emit(Resource.Error(errorMessage, 500))
                return@flow
            }
            val list = if (levelCode.isNullOrBlank()) {
                topicsByLevel.values.flatten()
            } else {
                topicsByLevel[levelCode.uppercase()].orEmpty()
            }
            if (list.isEmpty()) {
                emit(Resource.Empty)
            } else {
                emit(Resource.Success(list))
            }
        }

        override fun getTopicDetail(identifier: String): Flow<Resource<Topic>> = flow {
            emit(Resource.Loading)
            if (shouldFailTopicDetail) {
                emit(Resource.Error(errorMessage, 404))
                return@flow
            }
            if (returnEmptyTopicDetail) {
                emit(Resource.Empty)
                return@flow
            }
            val found = topicsByLevel.values.flatten().find {
                it.slug == identifier || it.id == identifier
            }
            if (found != null) {
                emit(Resource.Success(found))
            } else {
                emit(Resource.Error("Topic not found", 404))
            }
        }
    }
}
