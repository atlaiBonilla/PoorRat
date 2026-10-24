package com.poorrat.gotcha

import com.poorrat.gotcha.ui.shop.ShopNotificationHelper
import com.poorrat.gotcha.ui.shop.ShopProduct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ShopUnitTest {

    @Test
    fun testShopProductCreation() {
        val product = ShopProduct(
            id = 1L,
            name = "PoorRat Oversized Heavy Tee",
            category = "Apparel",
            price = "$38.00"
        )
        assertEquals(1L, product.id)
        assertEquals("PoorRat Oversized Heavy Tee", product.name)
        assertEquals("$38.00", product.price)
    }

    @Test
    fun testShopNotificationChannelId() {
        assertNotNull(ShopNotificationHelper.CHANNEL_ID)
        assertEquals("poorrat_shop_channel", ShopNotificationHelper.CHANNEL_ID)
    }
}
