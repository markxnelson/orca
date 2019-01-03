/*
 * Copyright (c) 2017, 2018, Oracle Corporation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the Apache License Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * If a copy of the Apache License Version 2.0 was not distributed with this file,
 * You can obtain one at https://www.apache.org/licenses/LICENSE-2.0.html
 */

package com.netflix.spinnaker.orca.clouddriver.tasks.providers.oracle

import com.netflix.spinnaker.orca.clouddriver.MortService
import com.netflix.spinnaker.orca.pipeline.model.Execution
import com.netflix.spinnaker.orca.pipeline.model.Stage
import retrofit.RetrofitError
import retrofit.client.Response
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class OracleSecurityGroupUpserterSpec extends Specification {

  private static final String PROVIDER = "oracle"

  @Subject
  OracleSecurityGroupUpserter upserter

  def "should return operations and extra outputs"() {
    given:
    upserter = new OracleSecurityGroupUpserter()
    def ctx = [
      name: "test-security-group",
      region: "global",
      credentials: "abc",
    ]
    def stage = new Stage(Execution.newPipeline("orca"), "whatever", ctx)

    when:
    def results = upserter.getOperationContext(stage)

    then:
    results

    def ops = results.operations
    ops.size() == 1
    (ops[0] as Map).upsertSecurityGroup == ctx

    def extraOutputs = results.extraOutput
    List<MortService.SecurityGroup> targets = extraOutputs.targets
    targets.size() == 1
    targets[0].name == "test-security-group"
    targets[0].region == "global"
    targets[0].accountName == "abc"
  }

  def "should return the correct result if the security group has been upserted"() {
    given:
    MortService.SecurityGroup sg = new MortService.SecurityGroup(name: "test-security-group",
      region: "global",
      accountName: "abc",
      inboundRules: [])
    MortService mortService = Mock(MortService)

    def ctx = [
      name: "test-security-group",
      region: "global",
      credentials: "abc",
      sourceRanges: [],
      ipIngress: []
    ]
    def stage = new Stage(Execution.newPipeline("orca"), "whatever", ctx)
    upserter = new OracleSecurityGroupUpserter(mortService: mortService)

    when:
    def result = upserter.isSecurityGroupUpserted(sg, stage)

    then:
    1 * mortService.getSecurityGroup("abc", PROVIDER, "test-security-group", "global") >> sg
    result

    when:
    result = upserter.isSecurityGroupUpserted(sg, stage)

    then:
    1 * mortService.getSecurityGroup("abc", PROVIDER, "test-security-group", "global") >> null
    !result

    when:
    result = upserter.isSecurityGroupUpserted(sg, stage)

    then:
    1 * mortService.getSecurityGroup("abc", PROVIDER, "test-security-group", "global") >> {
      throw RetrofitError.httpError("/", new Response("", 404, "", [], null), null, null)
    }
    !result

    when:
    upserter.isSecurityGroupUpserted(sg, stage)

    then:
    1 * mortService.getSecurityGroup("abc", PROVIDER, "test-security-group", "global") >> {
      throw RetrofitError.httpError("/", new Response("", 400, "", [], null), null, null)
    }
    thrown(RetrofitError)
  }
}
