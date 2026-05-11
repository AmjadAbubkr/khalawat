package com.khalawat.android.persistence

import androidx.room.TypeConverter
import com.khalawat.android.escalation.EscalationStage

class Converters {
    @TypeConverter
    fun fromEscalationStage(stage: EscalationStage): String = stage.name

    @TypeConverter
    fun toEscalationStage(name: String): EscalationStage =
        EscalationStage.valueOf(name)
}
