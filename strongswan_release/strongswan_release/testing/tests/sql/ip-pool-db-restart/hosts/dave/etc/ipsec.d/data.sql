/* Identities */

INSERT INTO identities (
  type, data
) VALUES ( /* C=CH, O=Linux strongSwan, CN=strongSwan Root CA */
  9, X'3045310B300906035504061302434831193017060355040A13104C696E7578207374726F6E675377616E311B3019060355040313127374726F6E675377616E20526F6F74204341'
 );

INSERT INTO identities (
  type, data
) VALUES ( /* subjkey of 'C=CH, O=Linux strongSwan, CN=strongSwan Root CA' */
  11, X'5da7dd700651327ee7b66db3b5e5e060ea2e4def'
 );

INSERT INTO identities (
  type, data
) VALUES ( /* keyid of 'C=CH, O=Linux strongSwan, CN=strongSwan Root CA' */
  11, X'ae096b87b44886d3b820978623dabd0eae22ebbc'
 );

INSERT INTO identities (
  type, data
) VALUES ( /* dave@strongswan.org */
  3, X'64617665407374726f6e677377616e2e6f7267'
 );

INSERT INTO identities (
  type, data
) VALUES ( /* subjkey of 'C=CH, O=Linux strongSwan, CN=dave@strongswan.org' */
  11, X'ec16639928815e01cc0227c0b9cb1feab7987037'
 );

INSERT INTO identities (
  type, data
) VALUES ( /* moon.strongswan.org */
  2, X'6d6f6f6e2e7374726f6e677377616e2e6f7267'
 );

/* Certificates */

INSERT INTO certificates (
   type, keytype, data
) VALUES ( /* C=CH, O=Linux strongSwan, CN=strongSwan Root CA */
  1, 1, X'308203b8308202a0a003020102020100300d06092a864886f70d01010b05003045310b300906035504061302434831193017060355040a13104c696e7578207374726f6e675377616e311b3019060355040313127374726f6e675377616e20526f6f74204341301e170d3034303931303130303131385a170d3139303930373130303131385a3045310b300906035504061302434831193017060355040a13104c696e7578207374726f6e675377616e311b3019060355040313127374726f6e675377616e20526f6f7420434130820122300d06092a864886f70d01010105000382010f003082010a0282010100bff25f62ea3d566e58b3c87a49caf3ac61cfa96377734d842db3f8fd6ea023f7b0132e66265012317386729c6d7c427a8d9f167be138e8ebae2b12b95933baef36a315c3ddf224cee4bb9bd578135d0467382629621ff96b8d45f6e002e5083662dce181805c140b3f2ce93f83aee3c861cff610a39f0189cb3a3c7cb9bf7e2a09544e2170efaa18fdd4ff20fa94be176d7fecff821f68d17152041d9b46f0cfcfc1e4cf43de5d3f3a587763afe9267f53b11699b3264fc55c5189f5682871166cb98307950569641fa30ffb50de134fed2f973cef1a392827862bc4ddaa97bbb01442e293c41070d07224d4be47ae2753eb2bed4bc1da91c68ec780c4620f0f0203010001a381b23081af30120603551d130101ff040830060101ff020101300b0603551d0f040403020106301d0603551d0e041604145da7dd700651327ee7b66db3b5e5e060ea2e4def306d0603551d230466306480145da7dd700651327ee7b66db3b5e5e060ea2e4defa149a4473045310b300906035504061302434831193017060355040a13104c696e7578207374726f6e675377616e311b3019060355040313127374726f6e675377616e20526f6f74204341820100300d06092a864886f70d01010b0500038201010023929aa101b412d1f5a577532088f209b34798a72ed7bd6945d74beaa2b3a1768764ad7f8b0df8d97a1a3ed1102e92a5f107e3059dc2250be49d02261ca83a342e0e5de7d43c37744e3fcea3197720ca1184d4ef94e6beeb0d241746b0b92b7fb1004c08e88bf9eb4ce60f3e149466f3e9fc3f98bce449f448f9d465e52b59f0101e6203cfad0d89e23509fa043d4c12021e8f32be7db8b2edbada641d64aa1a04af64a2ee5b814a753dd76b30e3de04f3c6b61166e632f8364d51cf3730a9564a4d93b9227c28b09b0f5595d92a632f72fe509a129ca9ee54df2b0edc6c3d38564f10256efcd8be82b2ec64977e3a6f5ef098eaa7f00662a6cded16cb80637c'
);

INSERT INTO certificates (
   type, keytype, data
) VALUES ( /* C=CH, O=Linux strongSwan, CN=dave@strongswan.org */
  1, 1, X'308204223082030aa003020102020131300d06092a864886f70d01010b05003045310b300906035504061302434831193017060355040a13104c696e7578207374726f6e675377616e311b3019060355040313127374726f6e675377616e20526f6f74204341301e170d3134303832373135313230325a170d3139303832363135313230325a305b310b300906035504061302434831193017060355040a13104c696e7578207374726f6e675377616e31133011060355040b130a4163636f756e74696e67311c301a0603550403141364617665407374726f6e677377616e2e6f726730820122300d06092a864886f70d01010105000382010f003082010a0282010100d63cdc4dc584cd5fb5e205add1d242130f196297d6083e254f3d20ad7be17fe4fd80c1f1f6fc774a266f02af82053b93929b01c9da5411b402f4666feaa45dd45988402524ea91d98adf941f8e30dedb9cf98341e908d3d4f30c9b7d6b50b5f5e2319942768760de0c0127c6ba69d70b0a9d605de3c31e6218e4004ad1871f00f199416e4772190243fb2f06b69d22592e2bcfc6a2190d2f612f8ff435643096db1a19766aac1563e177df9fff2d51b6e38fb2cd74dfd68f1a2f03e5d7e3c77206af37e33beba6376ea239607821d821094c26817f8ce8a1305243a4ebd5c43907ffd5e75f49d71fee87fff60a91105db15816253790b9934cfcfda5c99e56430203010001a38201053082010130090603551d1304023000300b0603551d0f0404030203a8301d0603551d0e04160414ec16639928815e01cc0227c0b9cb1feab7987037306d0603551d230466306480145da7dd700651327ee7b66db3b5e5e060ea2e4defa149a4473045310b300906035504061302434831193017060355040a13104c696e7578207374726f6e675377616e311b3019060355040313127374726f6e675377616e20526f6f74204341820100301e0603551d1104173015811364617665407374726f6e677377616e2e6f726730390603551d1f04323030302ea02ca02a8628687474703a2f2f63726c2e7374726f6e677377616e2e6f72672f7374726f6e677377616e2e63726c300d06092a864886f70d01010b0500038201010042a0be7f8ebc627b4aeaac2c0a500f53043041281319f71e707404233fd2fc6f6d10e120e9d6f023845afe031828c3bea9a8768b0eb77ba85284b01b653741b965739a548d3196869e7f52b5d779a44af62a6ef519362fda4dbddbb48e27ac27ed8f9f0400cf1396f6168cc3a22d83c084dfed7ce4d3694ce862e459f6feb77c762edd488cbcda23034bb139eaf245175b71079cbb89361010a39a436ef8490da0b36f18e0b8b048eaec6cb89f9db1793b8efeb23b788b7b0944b7f63f4eb6229025f2d5221d18cfea39963885d0eca8c64a581bf729a1589bd5165da8bd43b07116710c5807ef12486fed78bc62348671bcf1d47b3be2d3d1de92cc5dbc0ef7'
);

INSERT INTO certificate_identity (
  certificate, identity
) VALUES (
  1, 1
);

INSERT INTO certificate_identity (
  certificate, identity
) VALUES (
  1, 2
);

INSERT INTO certificate_identity (
  certificate, identity
) VALUES (
  1, 3
);

INSERT INTO certificate_identity (
  certificate, identity
) VALUES (
  2, 4 
);

INSERT INTO certificate_identity (
  certificate, identity
) VALUES (
  2, 5 
);

/* Private Keys */

INSERT INTO private_keys (
   type, data
) VALUES ( /* key of 'C=CH, O=Linux strongSwan, CN=dave@strongswan.org' */
  1, X'308204a40201000282010100d63cdc4dc584cd5fb5e205add1d242130f196297d6083e254f3d20ad7be17fe4fd80c1f1f6fc774a266f02af82053b93929b01c9da5411b402f4666feaa45dd45988402524ea91d98adf941f8e30dedb9cf98341e908d3d4f30c9b7d6b50b5f5e2319942768760de0c0127c6ba69d70b0a9d605de3c31e6218e4004ad1871f00f199416e4772190243fb2f06b69d22592e2bcfc6a2190d2f612f8ff435643096db1a19766aac1563e177df9fff2d51b6e38fb2cd74dfd68f1a2f03e5d7e3c77206af37e33beba6376ea239607821d821094c26817f8ce8a1305243a4ebd5c43907ffd5e75f49d71fee87fff60a91105db15816253790b9934cfcfda5c99e564302030100010282010100b628d1791f35281af55fd54c5257e0eeababfdf4f47bd5bb8b808244804ce09e281e4eca1a23d89a27cbc410cf10fbf32278c2308b7f67fe137513949d825c75ae1af7cf9d7005cb5f1573b0d10a24b3bee938fdd5d337aa9d6e5ed769641b1392dc715fddc08650c70c4471e9170d4dca1cbe4f96d5d503bbfe41715c6ef3a01e1b304b7f7d0f2ccd2192dfce9c3e5c5663c93a1284652fb0b50b68cfb093b6a20a4085bc37717b180255ecc5b8e10ffbc7dd68a8592775bc6a392da9b4cf36b391d3758a16f5c7b664727db3cfe2dce317f15bf26e9ba2592c785a22dcde728256b87af70a2f3ba2d30434e60c04f1f0e9ace4163d542017d72a48924ebb6102818100fef9ec239ad2bb059962fe1b9327fca60b47ff83c8238213e9976fc8443efb640faaa69447d1e65f477e7f004683858ab5d25b082d1582071eb294e10b69fe4009823f7a329a66b0e755f51a3ddbba2f367a7a5171903cce53fde7a666d33f64633f2541dd4d66b9645a6db2039c371e9fd2b84ef8b6ce3c2c733c1c2df1b0bd02818100d71910a4d9045e8c4ff85aa9bfb5fe2208a12ed123e0279e21d8dd623aca860a1094cbc4fceb104caf4d6bf2a302280365d7d603670ef76c748c98f435c7627e1286c79b16162b433723cc4dd7dc91d71d21003dc85dab8b2bcb43e8c8b66f5eb82703bc7faa577e08983b53f2f575acb6105ff5e005eb6cfa065b17495a12ff028180722a35313c015efc02d1f035288e9ef139dbe2ec7e90678dc2e9ab83600b53445a3f6e96b611a5b4c3ae3fef3fa36407d7a1786110ceb089bf94f6544d68080f64328d79896ed8cc1cc8c0161fdc6261016395a121d81361cef9c0df20eb35571d990875954132dd8fb16dac67316afcab530e734da98c6e24f8f3d12a412d810281810087313ad3d18ebc7a461d1ba6cb56f0b6a563e15013d3643330a4e46843f95c8be0e614b8e81a3a31689129c2e40f8be4aed31cb120ca385ad35a371c6edcb59bf5bafea40c1abcf04f2cd1d12f5673d7977851758384d437f1bb9d20275efadab6b5d6d4580d515666c27faf80ea241efc83883cbaa41dd76dc226e898f2e33302818079d917d98e48201e6a1fd3fef3c1af4c1dd79c74b783db0d54d41f3b6febb849e74487f300241e6430472d6a1dacb57fa32c93f897431e65f41fdf4d0cf8206eeaaf17ca0c3cdab5abf5f3229bc4b18fad56b3e823e46ed8d8ca373a1994675609d01aefe308285bdfcd86fec468e25d69112a5ba275031d8642620597ef2f2c'
);

INSERT INTO private_key_identity (
  private_key, identity
) VALUES (
  1, 4 
);

INSERT INTO private_key_identity (
  private_key, identity
) VALUES (
  1, 5 
);

/* Configurations */

INSERT INTO ike_configs (
  local, remote
) VALUES (
  'PH_IP_DAVE', 'PH_IP_MOON'
);

INSERT INTO peer_configs (
  name, ike_cfg, local_id, remote_id, virtual
) VALUES (
  'home', 1, 4, 6, '0.0.0.0'
);

INSERT INTO child_configs (
  name, updown
) VALUES (
  'home', '/usr/local/libexec/ipsec/_updown iptables'
);

INSERT INTO peer_config_child_config (
  peer_cfg, child_cfg
) VALUES (
  1, 1
);

INSERT INTO traffic_selectors (
  type, start_addr, end_addr
) VALUES ( /* 10.1.0.0/16 */
  7, X'0a010000', X'0a01ffff'
);

INSERT INTO traffic_selectors (
  type
) VALUES ( /* dynamic/32 */
  7
);

INSERT INTO child_config_traffic_selector (
  child_cfg, traffic_selector, kind
) VALUES (
  1, 1, 1
);

INSERT INTO child_config_traffic_selector (
	child_cfg, traffic_selector, kind
) VALUES (
  1, 2, 2
);

