package com.itech.innogas

data class UserDBStructure(
    var email: String? = null,
    var first_name: String? = null,
    var last_name: String? = null,
    var password: String? = null,
    var user_type: String? = null,
    var profile_pic: String? = null,
    var uid: String? = null
)
