package org.churchpresenter.hymn

import org.churchpresenter.core.models.schedule.ScheduleItem
import java.io.File

/**
 * The open hymn as the schedule row that identifies it — the same shape Add to Schedule builds.
 *
 * A conversion of its own rather than a helper beside the composable it serves: the conversion is
 * data, the pane file is layout, and that file is as long as one file should be.
 */
internal fun hymnPresentationRow(folder: File, slideCount: Int): ScheduleItem.PresentationItem =
    ScheduleItem.PresentationItem(
        id = java.util.UUID.randomUUID().toString(),
        filePath = folder.absolutePath,
        fileName = folder.name,
        slideCount = slideCount,
        fileType = "images",
        hymnNumber = folder.name,
    )
