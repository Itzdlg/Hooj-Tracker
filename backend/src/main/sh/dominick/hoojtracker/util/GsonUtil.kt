package sh.dominick.hoojtracker.util

import sh.dominick.hoojtracker.gson

fun Any.toJsonTree()
    = gson.toJsonTree(this)

fun Any.toJsonObject()
    = toJsonTree().asJsonObject