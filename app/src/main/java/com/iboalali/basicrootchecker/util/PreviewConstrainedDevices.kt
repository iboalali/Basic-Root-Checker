package com.iboalali.basicrootchecker.util

import androidx.compose.ui.tooling.preview.Preview

/**
 * Stress-test previews for two extreme, constrained device sizes. Each device isolates a single
 * constraint by pairing the tested dimension with a normal phone-sized counterpart:
 *
 *  - **Short** — 448px tall (paired with a normal 411px width) surfaces vertical compression and
 *    whether content scrolls instead of clipping.
 *  - **Narrow** — 288px wide (paired with a normal 891px height) surfaces text wrapping, FAB
 *    placement, and dialog/row layout problems.
 *
 * `dpi=160` keeps px and dp 1:1, so the numbers map directly to layout space. Raise the dpi to
 * simulate a higher-density tiny panel (same px, fewer dp of room).
 */
@Preview(
    name = "Short - 448px height",
    group = "Constrained devices",
    showBackground = true,
    device = "spec:width=411px,height=448px,dpi=160",
)
@Preview(
    name = "Narrow - 288px width",
    group = "Constrained devices",
    showBackground = true,
    device = "spec:width=288px,height=891px,dpi=160",
)
@Preview(
    name = "Narrow and Short - 288px width and 448px height",
    group = "Constrained devices",
    showBackground = true,
    device = "spec:width=288px,height=448px,dpi=160",
)
annotation class PreviewConstrainedDevices
