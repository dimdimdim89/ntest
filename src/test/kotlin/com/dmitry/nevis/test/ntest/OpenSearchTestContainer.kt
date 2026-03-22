package com.dmitry.nevis.test.ntest

import org.opensearch.testcontainers.OpenSearchContainer
import org.testcontainers.utility.DockerImageName

private const val OPEN_SEARCH_IMAGE = "opensearchproject/opensearch:3.0.0"

class OpenSearchTestContainer :
	OpenSearchContainer<OpenSearchTestContainer>(DockerImageName.parse(OPEN_SEARCH_IMAGE))
