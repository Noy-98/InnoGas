package com.itech.innogas

import java.io.Serializable

data class ProductsDBStructure(
    val p_id: String = "", // Primary key or unique identifier
    val product_name: String = "",
    val product_stock: String = "",
    val product_type: String = "",
    val product_price: String = "",
    val product_description: String = "",
    var product_image: String? = null
): Serializable
