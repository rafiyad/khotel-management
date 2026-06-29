# Known Issues / Deferred

Tracked issues that are understood but intentionally **not fixed yet**.

---

## 1. Room images leak to USER via the room embed

**Status:** Open — deferred (2026-06-26)
**Area:** Authorization / Room + Image modules

**Context:** Facility and image endpoints were locked down to HOTEL_OWNER/ADMIN (USER and
anonymous get 403/401 on `/facility/**`, `/image/**`, and the hotel/room facility + image
endpoints).

**The gap:** `GET /hotel/{hotelId}/room/{roomId}` and the room list `GET /hotel/{hotelId}/room`
are **public** (so visitors can browse rooms) and their responses **embed the room's images**
(`Room.images` — populated from `khotel_attachment`). So a USER (or anonymous) still sees image
**URLs** in the room payload, even though the dedicated `/image/...` endpoints are now blocked.

**Why it matters:** "images hidden from USER" is only partially true — the dedicated endpoints
are blocked, but the embed still exposes the URLs. If the bucket is/becomes public, those URLs
are directly fetchable.

**Possible fix (when picked up):** strip `images` from the room response for callers who are not
the hotel's owner or an admin (i.e. only embed images for owner/admin reads), or stop embedding
images on public room reads and require the dedicated (now-protected) image endpoints.

**Decision:** Left as-is for now per product call — room photos are arguably desirable for
visitors browsing rooms. Revisit if images must be fully owner/admin-only.



## 2. new (bug) need to fix next time

1. Jpeg format showing 500 error with no reader found 
2. hotel search don't include image in its profile
3. if 2 image with same filename stores in the bucket, shows single same name image and stores only one image and
 when deleted one file, as there is one file in the bucket, so that file got deleted, and we can't access the other file