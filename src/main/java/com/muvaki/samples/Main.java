package com.muvaki.samples;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.dlp.v2.DlpServiceClient;
import com.google.privacy.dlp.v2.CreateDeidentifyTemplateRequest;
import com.google.privacy.dlp.v2.DeidentifyTemplate;
import com.google.privacy.dlp.v2.FieldId;
import com.google.privacy.dlp.v2.GetDeidentifyTemplateRequest;
import com.google.privacy.dlp.v2.ProjectName;
import com.google.privacy.dlp.v2.ReidentifyContentRequest;
import com.google.privacy.dlp.v2.ReidentifyContentResponse;
import com.muvaki.samples.services.KMSService;
import com.muvaki.samples.utils.Utils;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.apache.commons.cli.ParseException;

public class Main {

//  private static final String CREATE_TEMPLATE_OPTION = "ct";
//  private static final String REIDENTIFY_OPTION = "rid";

  private static final String CREATE_TEMPLATE_FPE_OPTION = "FPE";
  private static final String CREATE_TEMPLATE_CRYPTOHASH_OPTION = "CRYPTOHASH";
  private static final String REIDENTIFY_OPTION = "REIDENTIFY";

  public static void main(String args[])
      throws ParseException, IOException, NoSuchAlgorithmException, InterruptedException {

    /**
     * 1) Create FPE.
     * 2) Create CryptoHash.
     * 3) Reidentify.
     */

    String command = args[0];

    if (command.equals(CREATE_TEMPLATE_FPE_OPTION) || command
        .equals(CREATE_TEMPLATE_CRYPTOHASH_OPTION)) {
      final List<FieldId> piiFields = Utils.getPIIFields(args[1]);
      final String keyName = args[2];
      final String projectId = args[3];
      final String templateId = args[4];

      DeidentifyTemplate deidentifyTemplate;

      if (command.equals(CREATE_TEMPLATE_FPE_OPTION)) {
        final KMSService kmsService = new KMSService();
        final KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        final SecretKey secretKey = keyGenerator.generateKey();

        final String cipherText = kmsService.encrypt(keyName, secretKey.getEncoded());

        deidentifyTemplate = Utils
            .createKMSWrappedFfxFpeConfigTemplate(piiFields, cipherText, keyName);

      } else {

        deidentifyTemplate = Utils
            .createCryptoHashDeidentifyTemplate(piiFields, keyName);

      }

      try (DlpServiceClient dlpServiceClient = DlpServiceClient.create()) {

        //CREATE.
        final CreateDeidentifyTemplateRequest createDeidentifyTemplateRequest = CreateDeidentifyTemplateRequest
            .newBuilder()
            .setParent(ProjectName.of(projectId).toString())
            .setTemplateId(templateId)
            .setDeidentifyTemplate(
                deidentifyTemplate
            ).build();

        DeidentifyTemplate createdTemplate = dlpServiceClient
            .createDeidentifyTemplate(createDeidentifyTemplateRequest);

        System.out.println(String
            .format("Successfully created [%s] template: %s.", command, createdTemplate.getName()));

      }

    } else if (command.equals(REIDENTIFY_OPTION)) {
      final String projectId = args[1];
      final String templateId = args[2];
      final String bqDataset = args[3];
      final String bqTable = args[4];
      final String bqPKey = args[5];
      final String bqPValue = args[6];
      final List<FieldId> piiFields = Utils.getPIIFields(args[7]);

      try (DlpServiceClient dlpServiceClient = DlpServiceClient.create()) {

        final String templatePathName = Utils.getTemplateName(projectId, templateId);

        DeidentifyTemplate deidentifyTemplate = dlpServiceClient.getDeidentifyTemplate(
            GetDeidentifyTemplateRequest.newBuilder()
                .setName(templatePathName)
                .build());

        if (deidentifyTemplate == null) {
          throw new IllegalArgumentException(String.format("%d does not exist.", templatePathName));
        }

        BigQuery bq = BigQueryOptions.getDefaultInstance().getService();

        String query = String.format(
            "SELECT * FROM `%s.%s.%s` WHERE %s = \"%s\"",
            projectId,
            bqDataset,
            bqTable,
            bqPKey,
            bqPValue
        );

        System.out.println("Running the following query: " + query);
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
        Iterable<FieldValueList> fieldValueLists = bq.query(queryConfig).iterateAll();
        Table table = bq.getTable(TableId.of(projectId, bqDataset, bqTable));

        fieldValueLists.forEach(fieldValues -> {
          ReidentifyContentResponse reidentifyContentResponse = dlpServiceClient
              .reidentifyContent(ReidentifyContentRequest.newBuilder()
                  .setParent(ProjectName.of(projectId).toString())
                  .setReidentifyTemplateName(deidentifyTemplate.getName())
                  .setItem(Utils
                      .getContentItem(fieldValues, table.getDefinition().getSchema().getFields(),
                          piiFields)).build());
          Utils.readContentItem(reidentifyContentResponse.getItem());
        });
      }
    }
  }

}
