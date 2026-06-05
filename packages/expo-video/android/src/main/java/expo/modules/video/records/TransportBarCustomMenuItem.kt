package expo.modules.video.records

import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import expo.modules.kotlin.types.OptimizedRecord

@OptimizedRecord
data class TransportBarCustomMenuItem(
  @Field val id: String = "",
  @Field val icon: String = "",
  @Field val title: String = ""
) : Record
