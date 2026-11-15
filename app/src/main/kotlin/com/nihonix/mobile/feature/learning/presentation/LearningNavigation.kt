package com.nihonix.mobile.feature.learning.presentation

import android.net.Uri
import com.nihonix.mobile.feature.learning.domain.*

object LearningDestinations {
    const val LIST = "learning/{kind}?topicId={topicId}&levelCode={levelCode}&parentId={parentId}"
    const val DETAIL = "learning_detail/{kind}/{identifier}"
    fun list(kind: ContentKind,filter: LearningFilter = LearningFilter()) =
        "learning/${kind.wire}?topicId=${Uri.encode(filter.topicId.orEmpty())}&levelCode=${Uri.encode(filter.levelCode.orEmpty())}&parentId=${Uri.encode(filter.parentId.orEmpty())}"
    fun detail(kind: ContentKind,key: String) = "learning_detail/${kind.wire}/${Uri.encode(key)}"
}
