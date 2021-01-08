package com.manan.Storm;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import org.apache.storm.topology.base.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Keeps stats on word count, calculates and prints top list every
 * logIntervalSec seconds to stdout
 */

public class WordCounterBolt extends BaseRichBolt {

	private static final long serialVersionUID = 2706047697068872387L;

	private static final Logger logger = LoggerFactory.getLogger(WordCounterBolt.class);

	/** Number of seconds before the top list will be logged to stdout. */
	private final long logIntervalSec;

	/** Number of seconds before the top list will be cleared. */
	private final long clearIntervalSec;

	/** Number of top words to store in stats. */
	private final int topListSize;

	private Map<String, Long> counter;
	private long lastLogTime;
	private long lastClearTime;

	private OutputCollector collector;

	public WordCounterBolt(long logIntervalSec, long clearIntervalSec, int topListSize) {
		this.logIntervalSec = logIntervalSec;
		this.clearIntervalSec = clearIntervalSec;
		this.topListSize = topListSize;
	}

	@Override
	public void prepare(Map map, TopologyContext topologyContext, OutputCollector collector) {
		counter = new HashMap<String, Long>();
		lastLogTime = System.currentTimeMillis();
		lastClearTime = System.currentTimeMillis();
		this.collector = collector;
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
	}

	@Override
	public void execute(Tuple input) {
		if (input.getFields().contains("word")) {
			String word = (String) input.getValueByField("word");
			Long count = counter.get(word);
			count = count == null ? 1L : count + 1;
			counter.put(word, count);

			long now = System.currentTimeMillis();
			long logPeriodSec = (now - lastLogTime) / 1000;
			if (logPeriodSec > logIntervalSec) {
				logger.info("\n\n");
				logger.info("Word count: " + counter.size());

				publishTopList();
				lastLogTime = now;
			}
			collector.ack(input);
		}

	}

	private void publishTopList() {
		// calculate top list:
		SortedMap<Long, String> top = new TreeMap<Long, String>();
		for (Map.Entry<String, Long> entry : counter.entrySet()) {
			long count = entry.getValue();
			String word = entry.getKey();

			top.put(count, word);
			if (top.size() > topListSize) {
				top.remove(top.firstKey());
			}
		}

		// Output top list:
		for (Map.Entry<Long, String> entry : top.entrySet()) {
			logger.info(new StringBuilder("Top List - ").append(entry.getValue()).append('|').append(entry.getKey())
					.toString());
		}

		// Clear top list
		long now = System.currentTimeMillis();
		if (now - lastClearTime > clearIntervalSec * 1000) {
			counter.clear();
			lastClearTime = now;
		}
	}
}

