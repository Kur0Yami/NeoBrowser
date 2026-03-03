package com.neobrowser.tabs

data class Tab(val id:String,var url:String,var title:String,var groupId:String?=null,var isPinned:Boolean=false,var timestamp:Long=System.currentTimeMillis())
data class TabGroup(val id:String,val name:String,val color:Int)
