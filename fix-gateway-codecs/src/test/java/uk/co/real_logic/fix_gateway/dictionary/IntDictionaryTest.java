/*
 * Copyright 2015 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.fix_gateway.dictionary;

import org.junit.Before;
import org.junit.Test;
import uk.co.real_logic.agrona.collections.IntHashSet;
import uk.co.real_logic.fix_gateway.dictionary.ir.Dictionary;
import uk.co.real_logic.fix_gateway.dictionary.ir.Field;
import uk.co.real_logic.fix_gateway.dictionary.ir.Field.Type;
import uk.co.real_logic.fix_gateway.dictionary.ir.Message;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.co.real_logic.fix_gateway.dictionary.ir.Category.ADMIN;

public class IntDictionaryTest
{
    private Dictionary data;

    @Before
    public void createDataDictionary()
    {
        final Message heartbeat = new Message("Hearbeat", '0', ADMIN);
        heartbeat.requiredEntry(new Field(115, "OnBehalfOfCompID", Type.STRING));
        heartbeat.optionalEntry(new Field(112, "TestReqID", Type.STRING));

        final List<Message> messages = Arrays.asList(heartbeat);
        data = new Dictionary(messages, null, null, null, null, 4, 4);
    }

    @Test
    public void buildsValidationDictionaryForRequiredFields()
    {
        final IntDictionary intDictionary = IntDictionary.requiredFields(data);
        final IntHashSet heartbeat = intDictionary.values('0');

        assertThat(heartbeat, hasItem(115));
        assertThat(heartbeat, hasSize(1));
        assertTrue(intDictionary.contains('0', 115));
    }

    @Test
    public void buildsValidationDictionaryForAllFields()
    {
        final IntDictionary intDictionary = IntDictionary.allFields(data);
        final IntHashSet heartbeat = intDictionary.values('0');

        assertThat(heartbeat, hasItem(115));
        assertThat(heartbeat, hasItem(112));
        assertThat(heartbeat, hasSize(2));

        assertTrue(intDictionary.contains('0', 115));
        assertTrue(intDictionary.contains('0', 112));
    }
}
