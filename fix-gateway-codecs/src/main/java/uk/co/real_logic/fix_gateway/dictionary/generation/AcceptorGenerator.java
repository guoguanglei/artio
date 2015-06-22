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
package uk.co.real_logic.fix_gateway.dictionary.generation;

import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.agrona.generation.OutputManager;
import uk.co.real_logic.fix_gateway.dictionary.ir.Dictionary;
import uk.co.real_logic.fix_gateway.dictionary.ir.Message;
import uk.co.real_logic.fix_gateway.util.AsciiFlyweight;

import java.io.IOException;
import java.io.Writer;

import static uk.co.real_logic.fix_gateway.dictionary.generation.DecoderGenerator.decoderClassName;
import static uk.co.real_logic.fix_gateway.dictionary.generation.GenerationUtil.fileHeader;
import static uk.co.real_logic.fix_gateway.dictionary.generation.GenerationUtil.importFor;
import static uk.co.real_logic.sbe.generation.java.JavaUtil.formatPropertyName;

public class AcceptorGenerator
{
    public static final String ON_MESSAGE = "onMessage";
    public static final String DICTIONARY_DECODER = "DictionaryDecoder";
    public static final String DICTIONARY_ACCEPTOR = "DictionaryAcceptor";

    private final Dictionary dictionary;
    private final String packageName;
    private final OutputManager outputManager;

    public AcceptorGenerator(final Dictionary dictionary,
                             final String packageName,
                             final OutputManager outputManager)
    {
        this.dictionary = dictionary;
        this.packageName = packageName;
        this.outputManager = outputManager;
    }

    public void generate()
    {
        generateAcceptor();
        generateDecoder();
    }

    private void generateAcceptor()
    {
        try (final Writer acceptorOutput = outputManager.createOutput(DICTIONARY_ACCEPTOR))
        {
            generateAcceptorClass(acceptorOutput);

            for (final Message message : dictionary.messages())
            {
                generateAcceptorCallback(acceptorOutput, message);
            }

            generateAcceptorSuffix(acceptorOutput);
        }
        catch (IOException e)
        {
            LangUtil.rethrowUnchecked(e);
        }
    }

    private void generateAcceptorCallback(final Writer acceptorOutput, final Message message) throws IOException
    {
        acceptorOutput.append(String.format(
            "    void on%1$s(final %2$s decoder);\n\n",
            message.name(),
            decoderClassName(message)
        ));
    }

    private void generateAcceptorSuffix(final Writer acceptorOutput) throws IOException
    {
        acceptorOutput.append("\n}\n");
    }

    private void generateAcceptorClass(final Writer acceptorOutput) throws IOException
    {
        acceptorOutput.append(fileHeader(packageName));
        acceptorOutput.append(
            "\n" +
            "public interface " + DICTIONARY_ACCEPTOR + "\n" +
            "{\n\n"
        );
    }

    private void generateDecoder()
    {
        try (final Writer decoderOutput = outputManager.createOutput(DICTIONARY_DECODER))
        {
            generateDecoderClass(decoderOutput);

            for (final Message message : dictionary.messages())
            {
                generateDecoderField(decoderOutput, message);
            }

            generateDecoderOnMessage(decoderOutput);

            for (final Message message : dictionary.messages())
            {
                generateDecoderCase(decoderOutput, message);
            }

            generateDecoderSuffix(decoderOutput);
        }
        catch (IOException e)
        {
            LangUtil.rethrowUnchecked(e);
        }
    }

    private void generateDecoderSuffix(final Writer decoderOutput) throws IOException
    {
        decoderOutput.append(
            "        }\n" +
            "    }\n\n");

        decoderOutput.append("}\n");
    }

    private void generateDecoderOnMessage(final Writer decoderOutput) throws IOException
    {
        decoderOutput.append(
            "\n" +
            "    public " + DICTIONARY_DECODER + "(final " + DICTIONARY_ACCEPTOR + " acceptor)\n" +
            "    {\n" +
            "        this.acceptor = acceptor;\n" +
            "    }\n\n" +
            "    public void " + ON_MESSAGE + "(\n" +
            "        final AsciiFlyweight buffer,\n" +
            "        final int offset,\n" +
            "        final int length,\n" +
            "        final int messageType)\n" +
            "    {\n" +
            "        switch(messageType)\n" +
            "        {\n\n");
    }

    private void generateDecoderCase(final Writer decoderOutput, final Message message) throws IOException
    {
        decoderOutput.append(String.format(
            "        case %1$s.MESSAGE_TYPE:\n" +
            "            %2$s.decode(buffer, offset, length);\n" +
            "            acceptor.on%3$s(%2$s);\n" +
            "            %2$s.reset();\n" +
            "            break;\n\n",
            decoderClassName(message),
            formatPropertyName(message.name()),
            message.name()
        ));
    }

    private void generateDecoderField(final Writer decoderOutput, final Message message) throws IOException
    {
        decoderOutput.append(String.format(
            "    private final %1$s %2$s = new %1$s();\n",
            decoderClassName(message),
            formatPropertyName(message.name())
        ));
    }

    private void generateDecoderClass(final Writer decoderOutput) throws IOException
    {
        decoderOutput.append(fileHeader(packageName));
        decoderOutput.append(importFor(AsciiFlyweight.class));
        decoderOutput.append(
            "\n" +
            "public final class " + DICTIONARY_DECODER + "\n" +
            "{\n\n" +
            "    private final " + DICTIONARY_ACCEPTOR + " acceptor;\n\n");
    }

}
