import { Box, Skeleton } from '@mui/material'

export default function KpiSkeleton() {
  return (
    <Box sx={{ display: 'grid', gap: 2 }}>
      <Skeleton variant="text" width={200} height={32} />
      <Skeleton variant="rectangular" height={160} />
      <Skeleton variant="rectangular" height={220} />
      <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(180px,1fr))', gap: 2 }}>
        <Skeleton variant="rectangular" height={120} />
        <Skeleton variant="rectangular" height={120} />
        <Skeleton variant="rectangular" height={120} />
        <Skeleton variant="rectangular" height={120} />
      </Box>
    </Box>
  )
}
