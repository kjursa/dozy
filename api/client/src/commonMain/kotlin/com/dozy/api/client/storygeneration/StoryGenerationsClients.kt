package com.dozy.api.client.storygeneration

import com.dozy.api.client.storygeneration.v1.StoryGenerationsClient

interface StoryGenerationsClients { val v1: StoryGenerationsClient }
internal class DefaultStoryGenerationsClients(override val v1: StoryGenerationsClient) : StoryGenerationsClients
