package com.neobrowser.userscript

data class Userscript(
    val id:String=java.util.UUID.randomUUID().toString(),
    val name:String="Untitled", val description:String="",
    val matches:List<String>=listOf("*"), val excludes:List<String>=emptyList(),
    val runAt:String="document-end", val code:String="", val enabled:Boolean=true
)
