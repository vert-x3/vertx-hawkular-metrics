package io.vertx.kotlin.ext.metric.reporters.hawkular

import io.vertx.ext.metric.reporters.hawkular.MetricTagsMatch
import io.vertx.ext.metric.reporters.hawkular.MetricTagsMatch.MatchType

/**
 * A function providing a DSL for building [io.vertx.ext.metric.reporters.hawkular.MetricTagsMatch] objects.
 *
 * Tags to apply to any metric which name matches the criteria.
 *
 * @param tags  Set the tags to apply if metric name matches the criteria.
 * @param type  Set the type of matching to apply.
 * @param value  Set the matched value.
 *
 * <p/>
 * NOTE: This function has been automatically generated from the [io.vertx.ext.metric.reporters.hawkular.MetricTagsMatch original] using Vert.x codegen.
 */
fun MetricTagsMatch(
  tags: io.vertx.core.json.JsonObject? = null,
  type: MatchType? = null,
  value: String? = null): MetricTagsMatch = io.vertx.ext.metric.reporters.hawkular.MetricTagsMatch().apply {

  if (tags != null) {
    this.setTags(tags)
  }
  if (type != null) {
    this.setType(type)
  }
  if (value != null) {
    this.setValue(value)
  }
}

