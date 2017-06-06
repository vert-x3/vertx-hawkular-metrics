package io.vertx.kotlin.ext.hawkular

import io.vertx.ext.hawkular.MetricTagsMatch
import io.vertx.ext.hawkular.Match

/**
 * A function providing a DSL for building [io.vertx.ext.hawkular.MetricTagsMatch] objects.
 *
 * Tags to apply to any metric which name matches the criteria.
 *
 * @param match  Set the criteria for metric name.
 * @param tags  Set the tags to apply if metric name matches the criteria.
 *
 * <p/>
 * NOTE: This function has been automatically generated from the [io.vertx.ext.hawkular.MetricTagsMatch original] using Vert.x codegen.
 */
fun MetricTagsMatch(
  match: io.vertx.ext.hawkular.Match? = null,
  tags: io.vertx.core.json.JsonObject? = null): MetricTagsMatch = io.vertx.ext.hawkular.MetricTagsMatch().apply {

  if (match != null) {
    this.setMatch(match)
  }
  if (tags != null) {
    this.setTags(tags)
  }
}

