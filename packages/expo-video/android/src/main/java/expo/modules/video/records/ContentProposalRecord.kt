package expo.modules.video.records

import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import expo.modules.kotlin.types.OptimizedRecord

@OptimizedRecord
data class ContentProposalRecord(
  @Field val title: String = "",
  @Field val imageUrl: String? = null,
  @Field val showAtRemainingTime: Double = 0.0
) : Record
