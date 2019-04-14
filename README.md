# DLP Template PoC

The following DLP Tokenization PoC has 3 usages:

1) Create a Format Perserving Encryption (FPE) DLP template using a Customer Supplied Encryption Key (CSEK).
2) Create a CryptoHash using a random AES-256 key
3) Re-Identify a de-identified table column using an FPE template.

## Tech Requirements

- Java 8+
- Gradle

## Pre-requisites

- Create a symmetric Cloud KMS key: https://cloud.google.com/kms/docs/creating-keys
- Make sure your service account or user account has the correct IAM permissions.

## Getting Started

Clone the repo locally.

### Create FPE Template

In the parent directory, replace all the values in <> parentheses with what you want and run the command:

```sh
$ gradle run --args "FPE \
<comma_separated_list_pii_fields> \
projects/<kms_project_id>/locations/global/keyRings/<kms_keyring_name/cryptoKeys/<key_name> \
<dlp_project_id> \
<unique_dlp_template_name>"
```

### Create CryptoHash Template

In the parent directory, replace all the values in <> parentheses with what you want and run the command:

```sh
$ gradle run --args "CRYPTOHASH \
<comma_separated_list_pii_fields> \
<arbitrary_keyname> \
<dlp_project_id> \
<unique_dlp_template_name>"
```

### Re-identify using FPE Template

In the parent directory, replace all the values in <> parentheses with what you want and run the command:

```sh
$ gradle run --args "REIDENTIFY 
<dlp_project_id> \
<dlp_template_name> \
<bq_dataset_name> \
<bq_table_name> \
<bq_pkey> \
<bq_pkey_value> \
<comma_separated_list_pii_fields>"
```

Note that the bq_table_name project has to be the same as the dlp_project_id.

## TODO

If necessary, make it a proper CLI.


