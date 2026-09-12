package com.cochelper.app.ui.navigation

object Routes {
    const val HOME = "home"
    const val CHARACTERS = "characters"
    const val DICE = "dice"
    const val TOOLS = "tools"

    const val MODULE = "module/{moduleId}"
    const val ORIGINAL = "original/{moduleId}"
    const val FILES = "files/{moduleId}"
    const val INTRO = "intro/{moduleId}"
    const val TIMELINE = "timeline/{moduleId}"
    const val LOCATIONS = "locations/{moduleId}"
    const val LOCATION = "location/{locationId}"
    const val NPCS = "npcs/{moduleId}"
    const val PCS = "pcs/{moduleId}"
    const val PC = "pc/{pcId}?avatar={avatar}"
    const val NPC = "npc/{npcId}"
    const val CLUES = "clues"

    const val LOGIN = "login"
    const val COMBAT = "combat"
    const val CHASE = "chase"
    const val TIMER = "timer"

    fun module(id: Long) = "module/$id"
    fun module(id: Long, trans: String) = "module/$id?trans=$trans"
    fun original(id: Long) = "original/$id"
    fun files(id: Long) = "files/$id"
    fun intro(id: Long) = "intro/$id"
    fun timeline(id: Long) = "timeline/$id"
    fun locations(id: Long) = "locations/$id"
    fun location(id: Long) = "location/$id"
    fun npcs(id: Long) = "npcs/$id"
    fun pcs(id: Long) = "pcs/$id"
    fun pc(id: Long, avatar: Boolean = false) = "pc/$id?avatar=$avatar"
    fun npc(id: Long) = "npc/$id"
}
