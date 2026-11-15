package com.dozy.api.client.story

import com.dozy.api.client.story.v1.StoriesClient

interface StoriesClients { val v1: StoriesClient }
internal class DefaultStoriesClients(override val v1: StoriesClient) : StoriesClients
