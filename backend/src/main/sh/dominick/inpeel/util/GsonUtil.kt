package sh.dominick.inpeel.util

import sh.dominick.inpeel.gson

fun Any.toJsonTree()
    = gson.toJsonTree(this)

fun Any.toJsonObject()
    = toJsonTree().asJsonObject