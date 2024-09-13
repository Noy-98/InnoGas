package com.itech.innogas

data class UserDBStructure(
    var email: String? = null,
    var first_name: String? = null,
    var last_name: String? = null,
    var password: String? = null,
    var user_type: String? = null,
    var profile_pic: String? = "https://firebasestorage.googleapis.com/v0/b/innogasproj.appspot.com/o/profile_pictures%2Fdefault_pic.png?alt=media&token=e7e356d2-a820-4038-9914-d7156c45f1b0",
    var uid: String? = null
)
