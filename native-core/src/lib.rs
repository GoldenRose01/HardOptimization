use std::slice;

pub const HARDOPT_NATIVE_ABI_VERSION: u32 = 1;

#[unsafe(no_mangle)]
pub extern "C" fn hardopt_version() -> u32 {
    HARDOPT_NATIVE_ABI_VERSION
}

/// Fast off-heap 64-bit hash (FNV-1a / splitmix64 avalanche) for contiguous byte buffers.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn hardopt_hash64(ptr: *const u8, len: usize) -> u64 {
    if ptr.is_null() || len == 0 {
        return 0;
    }
    let bytes = unsafe { slice::from_raw_parts(ptr, len) };
    let mut h: u64 = 0xcbf29ce484222325;
    for &b in bytes {
        h ^= b as u64;
        h = h.wrapping_mul(0x100000001b3);
    }
    h ^= h >> 30;
    h = h.wrapping_mul(0xbf58476d1ce4e5b9);
    h ^= h >> 27;
    h = h.wrapping_mul(0x94d049bb133111eb);
    h ^ (h >> 31)
}

/// Batch AABB overlap filter over contiguous off-heap f64 arrays:
/// `aabbs` has `count * 6` f64 values: [minX, minY, minZ, maxX, maxY, maxZ] per entity.
/// Writes matching indices into `out_indices` (up to `out_cap`) and returns total matches written.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn hardopt_batch_aabb_overlaps(
    aabbs_ptr: *const f64,
    count: usize,
    q_min_x: f64,
    q_min_y: f64,
    q_min_z: f64,
    q_max_x: f64,
    q_max_y: f64,
    q_max_z: f64,
    out_indices_ptr: *mut u32,
    out_cap: usize,
) -> usize {
    if aabbs_ptr.is_null() || out_indices_ptr.is_null() || count == 0 || out_cap == 0 {
        return 0;
    }
    let aabbs = unsafe { slice::from_raw_parts(aabbs_ptr, count * 6) };
    let out = unsafe { slice::from_raw_parts_mut(out_indices_ptr, out_cap) };

    let mut written = 0usize;
    for i in 0..count {
        let base = i * 6;
        let min_x = aabbs[base];
        let min_y = aabbs[base + 1];
        let min_z = aabbs[base + 2];
        let max_x = aabbs[base + 3];
        let max_y = aabbs[base + 4];
        let max_z = aabbs[base + 5];

        if max_x > q_min_x
            && min_x < q_max_x
            && max_y > q_min_y
            && min_y < q_max_y
            && max_z > q_min_z
            && min_z < q_max_z
        {
            out[written] = i as u32;
            written += 1;
            if written == out_cap {
                break;
            }
        }
    }
    written
}
