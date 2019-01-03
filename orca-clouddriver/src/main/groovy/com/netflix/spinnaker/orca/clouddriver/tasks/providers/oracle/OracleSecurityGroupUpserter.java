package com.netflix.spinnaker.orca.clouddriver.tasks.providers.oracle;

import com.netflix.spinnaker.orca.clouddriver.MortService;
import com.netflix.spinnaker.orca.clouddriver.tasks.securitygroup.SecurityGroupUpserter;
import com.netflix.spinnaker.orca.clouddriver.utils.CloudProviderAware;
import com.netflix.spinnaker.orca.pipeline.model.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import retrofit.RetrofitError;
import retrofit.client.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OracleSecurityGroupUpserter implements SecurityGroupUpserter, CloudProviderAware {

  private final String cloudProvider = "oracle";

  @Autowired
  MortService mortService;

  @Override
  public String getCloudProvider() {
    return cloudProvider;
  }

  @Override
  public OperationContext getOperationContext(Stage stage) {
    LinkedHashMap<String, Map<String, Object>> opsMap = new LinkedHashMap<String, Map<String, Object>>(1);
    opsMap.put(SecurityGroupUpserter.OPERATION, stage.getContext());
    List<Map> opsList = new ArrayList<Map>(Arrays.asList(opsMap));

    MortService.SecurityGroup group = new MortService.SecurityGroup();
    group.setName(stage.getContext().get("name").toString());
    group.setRegion(stage.getContext().get("region").toString());
    group.setAccountName(getCredentials(stage));
    List<MortService.SecurityGroup> targetsList = new ArrayList<MortService.SecurityGroup>(Arrays.asList(group));
    LinkedHashMap<String, List<MortService.SecurityGroup>> targetsMap = new LinkedHashMap<String, List<MortService.SecurityGroup>>(1);
    targetsMap.put("targets", targetsList);

    OperationContext ctx = new OperationContext();
    ctx.setOperations(opsList);
    ctx.setExtraOutput(targetsMap);

    return ctx;
  }

  @Override
  public boolean isSecurityGroupUpserted(MortService.SecurityGroup upsertedSecurityGroup, Stage stage) {
    try {
      MortService.SecurityGroup group = mortService.getSecurityGroup(upsertedSecurityGroup.getAccountName(),
              cloudProvider,
              upsertedSecurityGroup.getName(),
              upsertedSecurityGroup.getRegion());
      return group != null;
    } catch (RetrofitError e) {
      final Response response = e.getResponse();
      if ((response == null ? null : response.getStatus()) != 404) {
        throw e;
      }
    }

    return false;
  }
}
