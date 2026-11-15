package com.nihonix.mobile.feature.flashcards.domain

data class CardFace(val text:String,val type:String="text",val meaning:String?=null)
data class CardDraft(val id:String,val front:CardFace,val back:CardFace,val order:Int=1)
data class FolderDraft(val id:String,val name:String,val description:String="",val order:Int=1)
data class SetDraft(val id:String,val folderId:String,val name:String,val cards:List<CardDraft>,val order:Int=1)
private val uuid=Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
fun validateFolder(draft:FolderDraft):String? = when {
    !uuid.matches(draft.id) -> "Invalid folder ID."
    draft.name.isBlank() || draft.name.length>255 -> "Name must contain 1–255 characters."
    draft.description.length>1000 -> "Description must be at most 1000 characters."
    draft.order !in 1..1000000 -> "Invalid order."
    else -> null
}
fun validateSet(draft:SetDraft):String? {
    if(!uuid.matches(draft.id)||!uuid.matches(draft.folderId)) return "Invalid folder or set ID."
    if(draft.name.isBlank()||draft.name.length>255) return "Name must contain 1–255 characters."
    if(draft.cards.size>200) return "A set supports at most 200 cards."
    if(draft.cards.map{it.id.lowercase()}.distinct().size!=draft.cards.size) return "Duplicate card IDs."
    for((index,card) in draft.cards.withIndex()) {
        if(!uuid.matches(card.id)) return "Invalid card ID."
        if(card.order !in 1..1000000) return "Invalid card order."
        for((side,face) in listOf("front" to card.front,"back" to card.back)) {
            if(face.text.isBlank()||face.text.length>4000||face.type!="text"||(face.meaning?.length ?: 0)>4000)
                return "Card ${index+1}: $side must contain 1–4000 characters."
        }
    }
    return null
}

/** Future generation produces drafts here; only explicit save persists them. */
data class GeneratedCardPreview(val cards:List<CardDraft>)
data class ReviewState(val ids:List<String>,val position:Int=0,val back:Boolean=false) {
    fun flip()=copy(back=!back)
    fun next()=copy(position=(position+1).coerceAtMost(ids.size),back=false)
    fun previous()=copy(position=(position-1).coerceAtLeast(0),back=false)
}
