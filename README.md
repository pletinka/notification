# App jobs:
Job 1: async connect: add the msg to the Sorted Set. One Sorted Set for all nodes per time unit - 100 milliseconds.
Job 2: sync connects: pull data from the Sorted Set and push them to the List every 100 milliseconds. It works with Redis transaction and provides right consistency. it is separate list for each node
Job 3: sync connect: blocked read from the List and print the message

# Data structure details:
1. Timeline records contain in Batches: every 100 milliseconds is a separate butch - it is a Sorted Set for all records in this "period". Like: 

Batch "1". Key: "2019-03-09 :54:15.5"
2019-03-09 :54:15.580
2019-03-09 :54:15.520
2019-03-09 :54:15.530

Batch "2": Key "2019-03-09 :54:15.6"
2019-03-09 :54:15.630
2019-03-09 :54:15.640

- The key is a "long" representation of "2019-03-09 :54:15.5".
- Values - text messages
- Score: "long" representation of full milliseconds "2019-03-09 :54:15.630" (for 1000 of milliseconds)

2. List - simple list as a Queue FIFO. Just collect the msg for processing

- Why Sorted Set? - as it provides natural order by the Score from the box. It provides the ability to use async Netty based controller to serve tons of requests from REST, but put them in the right order in the Redis by default.
- Why batches in Sorted Set for every 100 milliseconds instead of 1 huge Sorted Set? - the time complexity for ZRANGE is not OK for close to the infinite range: O(log(N)+M) - N is the number of elements in the sorted set and M the number of elements returned. Besides, it provides the "natural" batch distribution in the cluster 
- Why List? - provide the blocked queue functionality in FIFO mode with the right time complexity: BLPOP - O(1), LPUSH - O(1).

# Some other details:
- I track the last processing time in the separate key-value record for each node. Key: "node name" Value "2019-03-09 :54:15.6" - last processed time. Other more streaming variant - use BRPOPLPUSH to keep the last processed message in separate queues. But need one more Job to erase this additional queue for each data. It depends on the requirements and is part design compromises: operations on the node vs overhead data on the Redis cluster.
- I use async and sync Redis connections for different cases: sync - when should guarantee order, async - when order doesn't matter
- "nodeAlive" flag provides the status of the node. It is a part of the graceful shutdown workflow. It doesn't implement as it is out of scope for this task. It is always "true" in the code for now.
- Batch for 100 milliseconds - it is a configurable value.
- Job 2 use ScheduledExecutorService which guarantee, that task runs with a fixed rate 
- in case of adding a new node, it starts to work from the current time. But it is possible to catch all records from timeline: should just add the key to Redis with the predefined start time for this node. 
