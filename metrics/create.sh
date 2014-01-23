curl \
 -u javier@cozybit.com:2a7978e341b3c1706c77b8c46f9700063ef73779f3ef706bcb8091e25d515ff4 \
 -d 'gauges[0][name]=status' \
 -d 'gauges[0][value]=0' \
 -X POST https://metrics-api.librato.com/v1/metrics
