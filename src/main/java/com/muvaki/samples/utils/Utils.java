package com.muvaki.samples.utils;

import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.google.privacy.dlp.v2.ContentItem;
import com.google.privacy.dlp.v2.CryptoHashConfig;
import com.google.privacy.dlp.v2.CryptoKey;
import com.google.privacy.dlp.v2.CryptoReplaceFfxFpeConfig;
import com.google.privacy.dlp.v2.DeidentifyConfig;
import com.google.privacy.dlp.v2.DeidentifyTemplate;
import com.google.privacy.dlp.v2.FieldId;
import com.google.privacy.dlp.v2.FieldTransformation;
import com.google.privacy.dlp.v2.KmsWrappedCryptoKey;
import com.google.privacy.dlp.v2.PrimitiveTransformation;
import com.google.privacy.dlp.v2.RecordTransformations;
import com.google.privacy.dlp.v2.Table;
import com.google.privacy.dlp.v2.Table.Row;
import com.google.privacy.dlp.v2.TransientCryptoKey;
import com.google.privacy.dlp.v2.Value;
import com.google.protobuf.ByteString;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Utils {

  public static DeidentifyConfig createCryptoHashDeidentifyConfig(List<FieldId> piiFields,
      String cryptoKeyName) {
    final CryptoHashConfig cryptoHashConfig = CryptoHashConfig.newBuilder()
        .setCryptoKey(CryptoKey.newBuilder().setTransient(TransientCryptoKey.newBuilder()
            .setName(cryptoKeyName)).build()).build();

    final PrimitiveTransformation primitiveTransformation = PrimitiveTransformation.newBuilder()
        .setCryptoHashConfig(cryptoHashConfig).build();

    final FieldTransformation fieldTransformation = FieldTransformation.newBuilder()
        .setPrimitiveTransformation(primitiveTransformation).addAllFields(piiFields).build();

    final RecordTransformations recordTransformations = RecordTransformations.newBuilder()
        .addFieldTransformations(fieldTransformation).build();

    return DeidentifyConfig.newBuilder()
        .setRecordTransformations(recordTransformations).build();

  }

  public static DeidentifyTemplate createCryptoHashDeidentifyTemplate(List<FieldId> piiFields,
      String kmsKeyName) {
    return DeidentifyTemplate.newBuilder()
        .setDeidentifyConfig(createCryptoHashDeidentifyConfig(piiFields, kmsKeyName)).build();
  }

  public static DeidentifyConfig createKMSWrappedFfxFpeConfig(List<FieldId> piiFields,
      String kmsWrappedKey, String kmsKeyName) {
    final CryptoKey cryptoKey = CryptoKey.newBuilder()
        .setKmsWrapped(KmsWrappedCryptoKey.newBuilder()
            .setWrappedKey(ByteString.copyFrom(BaseEncoding.base64().decode(kmsWrappedKey)))
            .setCryptoKeyName(kmsKeyName)).build();

    final CryptoReplaceFfxFpeConfig cryptoReplaceFfxFpeConfig =
        CryptoReplaceFfxFpeConfig.newBuilder()
            .setCryptoKey(cryptoKey)
            .setCustomAlphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZ23456789=")
            .build();

    final PrimitiveTransformation primitiveTransformation = PrimitiveTransformation.newBuilder()
        .setCryptoReplaceFfxFpeConfig(cryptoReplaceFfxFpeConfig).build();

    final FieldTransformation fieldTransformation = FieldTransformation.newBuilder()
        .setPrimitiveTransformation(primitiveTransformation).addAllFields(piiFields).build();

    final RecordTransformations recordTransformations = RecordTransformations.newBuilder()
        .addFieldTransformations(fieldTransformation).build();

    return DeidentifyConfig.newBuilder()
        .setRecordTransformations(recordTransformations).build();
  }

  public static DeidentifyTemplate createKMSWrappedFfxFpeConfigTemplate(List<FieldId> piiFields,
      String kmsWrappedKey, String kmsKeyName) {
    return DeidentifyTemplate.newBuilder().setDeidentifyConfig(createKMSWrappedFfxFpeConfig(
        piiFields, kmsWrappedKey, kmsKeyName)
    ).build();
  }

  public static final String DEFAULT_PII_SEPARATOR = ",";

  public static List<FieldId> getPIIFields(String delimitedSeparatedList) {

    return Arrays.asList(delimitedSeparatedList.split(DEFAULT_PII_SEPARATOR)).stream()
        .map(fieldName -> FieldId.newBuilder().setName(fieldName.trim()).build())
        .collect(
            Collectors.toList());

  }

  public static String getTemplateName(String projectName, String templateId) {
    return String.format("projects/%s/deidentifyTemplates/%s",
        projectName,
        templateId
    );
  }

  public static ContentItem getContentItem(FieldValueList fieldValueList,
      FieldList fieldList, List<FieldId> piiFields) {
    Row.Builder rowBuilder = Row.newBuilder();

    List<FieldId> fieldsList = Lists.newArrayList();

    //Only gets the fields that are PII to re-identify.
    fieldList.forEach(field -> {
      String fieldName = field.getName();
      if (piiFields.stream().anyMatch(piiField -> piiField.getName().equals(fieldName))) {
        fieldsList.add(FieldId.newBuilder().setName(fieldName).build());
        FieldValue fieldValue = fieldValueList.get(fieldName);
        rowBuilder
            .addValues(Value.newBuilder().setStringValue(fieldValue.getStringValue()).build());
      }

    });

    Table.newBuilder().addAllHeaders(fieldsList).addRows(rowBuilder.build());

    return ContentItem.newBuilder()
        .setTable(
            Table.newBuilder()
                .addAllHeaders(fieldsList)
                .addRows(rowBuilder.build())
        ).build();

  }

  public static void readContentItem(ContentItem item) {
    List<Row> rowsList = item.getTable().getRowsList();
    rowsList.forEach((Row row) -> {
      row.getValuesList().forEach((Value value) -> System.out
          .println(new String(BaseEncoding.base32().decode(value.getStringValue())))
      );
    });
  }
}
