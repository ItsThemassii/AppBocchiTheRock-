package com.bocchifan.app.ui.characters

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocchifan.app.data.Character
import com.bocchifan.app.ui.components.rememberAssetImage
import com.bocchifan.app.ui.theme.Ink

/** Color de texto legible sobre el color del personaje. */
fun contentColorOn(background: Color): Color = if (background.luminance() > 0.45f) Ink else Color.White

/**
 * Retrato del personaje: su imagen de contenido/imagenes/personajes/<id> si existe;
 * si no, su inicial grande sobre su color.
 */
@Composable
fun CharacterPortrait(
    character: Character,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    borderWidth: Dp = 3.dp,
) {
    val image by rememberAssetImage("personajes/${character.id}")
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(character.color)
            .border(borderWidth, Ink, shape),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = image
        if (bitmap != null) {
            Image(
                bitmap,
                contentDescription = character.name,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            // Sin imagen: círculo más claro que la tarjeta, con la inicial en tinta.
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color.White.copy(alpha = 0.5f)),
            )
            Text(
                character.initial,
                color = Ink,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                fontSize = (size.value * 0.45f).sp,
            )
        }
    }
}
