/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.connectors.kafka.internal;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The ConsumerCallBridge simply calls the {@link KafkaConsumer#assign(java.util.Collection)} method.
 *
 * <p>This indirection is necessary, because Kafka broke binary compatibility between 0.9 and 0.10,
 * changing {@code assign(List)} to {@code assign(Collection)}.
 *
 * <p>Because of that, we need two versions whose compiled code goes against different method signatures.
 */
public class KafkaConsumerCallBridge010 extends KafkaConsumerCallBridge {

	private Date startupDate;

	public KafkaConsumerCallBridge010(Date startupDate) {
		super();
		this.startupDate = startupDate;
	}

	@Override
	public void assignPartitions(KafkaConsumer<?, ?> consumer, List<TopicPartition> topicPartitions) throws Exception {
		consumer.assign(topicPartitions);
	}

	@Override
	public void seekPartitionToBeginning(KafkaConsumer<?, ?> consumer, TopicPartition partition) {
		consumer.seekToBeginning(Collections.singletonList(partition));
	}

	@Override
	public void seekPartitionToEnd(KafkaConsumer<?, ?> consumer, TopicPartition partition) {
		consumer.seekToEnd(Collections.singletonList(partition));
	}

	@Override
	public void seekPartitionToDate(KafkaConsumer<?, ?> consumer, TopicPartition partition) {
		Map<TopicPartition, Long> partitionTimestampMap = new HashMap<>(1);
		partitionTimestampMap.put(partition, startupDate.getTime());

		Map<TopicPartition, OffsetAndTimestamp> topicPartitionOffsetMap = consumer.offsetsForTimes(partitionTimestampMap);
		OffsetAndTimestamp offsetAndTimestamp = null == topicPartitionOffsetMap ? null : topicPartitionOffsetMap.get(partition);
		if (null == offsetAndTimestamp) {
			seekPartitionToEnd(consumer, partition);
		} else {
			consumer.seek(partition, offsetAndTimestamp.offset());
		}
	}
}
