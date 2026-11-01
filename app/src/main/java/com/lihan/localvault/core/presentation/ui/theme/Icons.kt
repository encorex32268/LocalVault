package com.lihan.localvault.core.presentation.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.lihan.localvault.R

enum class IconsEnum(@field:DrawableRes val resId: Int) {
    USER(R.drawable.user),
    HOUSE(R.drawable.house),
    HOUSES(R.drawable.houses),
    KEY(R.drawable.key),
    CLAPPERBOARD(R.drawable.clapperboard),
    WIFI(R.drawable.wifi),
    WIFI_LOCK(R.drawable.wifi_lock),
    UTENSILS(R.drawable.utensils),
    GAMEPAD(R.drawable.gamepad),
    SWORDS(R.drawable.swords),
    LANDMARK(R.drawable.landmark),
    LOCK_KEYHOLE(R.drawable.lock_keyhole),
    MAIL(R.drawable.mail),
    MAP(R.drawable.map),
    MESSAGES_CIRCLE(R.drawable.messages_circle),
    PHONE(R.drawable.phone),
    SIGNAL(R.drawable.signal),
    SMARTPHONE(R.drawable.smartphone),
    BOT_MESSAGE_SQUARE(R.drawable.bot_message_square),
    BIT_COIN(R.drawable.bitcoin),
    BUILDING_COMPLEX(R.drawable.building_complex),
    CAKE(R.drawable.cake),
    CALENDAR(R.drawable.calendar),
    CAR(R.drawable.car),
    CONTACT(R.drawable.contact),
    CONTAINER(R.drawable.container),
    COPY(R.drawable.copy),
    CPU(R.drawable.cpu),
    CREDIT_CARD(R.drawable.credit_card),
    EYE(R.drawable.eye),
    GLOBE(R.drawable.globe),
    HEADPHONES(R.drawable.headphones),
    STAR(R.drawable.star),
    TOMBSTONE(R.drawable.tombstone),
    TRASH(R.drawable.trash),
}

object LocalVaultIcons {
    val Add: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.add)

    val Back: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.back)

    val Camera: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.ic_camera)

    val ChevronRight: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.chevron_right)

    val ChevronUp: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.chevron_up)

    val Close: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.x)

    val X: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.x)

    val Copy: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.copy)

    val Document: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.ic_document)

    val Edit: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.edit)

    val Eye: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.eye)

    val EyeOff: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.eye_off)

    val Fingerprint: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.fingerprint)

    val Image: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.ic_image)

    val Key: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.key)

    val LockKeyhole: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.lock_keyhole)

    val Moon: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.moon)

    val MoreVert: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.more_vert)

    val Paste: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.paste)

    val Save: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.save)

    val Search: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.search)

    val Sun: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.sun)

    val Trash: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.trash)

    val User: ImageVector
        @Composable
        get() = ImageVector.vectorResource(R.drawable.user)
}
