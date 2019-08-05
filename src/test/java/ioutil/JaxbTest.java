/*
 * Copyright 2017 National Bank of Belgium
 * 
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved 
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and 
 * limitations under the Licence.
 */
package ioutil;

import _test.ForwardingMarshaller;
import _test.sample.ParseAssertions;
import _test.sample.Person;
import java.io.IOException;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;
import _test.ForwardingUnmarshaller;
import _test.ForwardingXMLInputFactory;
import _test.StaxListener;
import _test.JaxbListener;
import _test.Meta;
import javax.xml.bind.Marshaller;
import static _test.sample.FormatAssertions.assertFormatterCompliance;
import static _test.sample.FormatAssertions.assertFormatterSafety;
import static _test.sample.ParseAssertions.assertParserCompliance;
import static _test.sample.ParseAssertions.assertParserSafety;
import static _test.sample.Person.BOOLS;
import static _test.sample.Person.ENCODINGS;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author Philippe Charles
 */
public class JaxbTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private final IO.Supplier<Unmarshaller> validUnmarshaller = () -> Jaxb.createUnmarshaller(Person.class);
    private final IO.Supplier<Marshaller> validMarshaller = () -> Jaxb.createMarshaller(Person.class);
    private final IO.Supplier<XMLInputFactory> validXxeFactory = XMLInputFactory::newFactory;

    @Test
    @SuppressWarnings("null")
    public void testCreateUnmarshaller() throws Exception {
        assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.createUnmarshaller((Class<?>) null));

        assertThatThrownBy(() -> Jaxb.createUnmarshaller(Runnable.class))
                .isInstanceOf(Xml.WrappedException.class)
                .hasCauseInstanceOf(JAXBException.class);

        assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.createUnmarshaller((JAXBContext) null));
    }

    @Test
    @SuppressWarnings("null")
    public void testCreateMarshaller() throws Exception {
        assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.createMarshaller((Class<?>) null));

        assertThatThrownBy(() -> Jaxb.createMarshaller(Runnable.class))
                .isInstanceOf(Xml.WrappedException.class)
                .hasCauseInstanceOf(JAXBException.class);

        assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.createMarshaller((JAXBContext) null));
    }

    @Test
    public void testXXE() throws Exception {
        Jaxb.Parser<Person> p = Jaxb.Parser.of(Person.class);
        ParseAssertions.testXXE(p, p.withIgnoreXXE(true));
    }

    @Test
    @SuppressWarnings("null")
    public void testParserOfClass() throws Exception {
        assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Parser.of((Class<?>) null));

        assertParserCompliance(Jaxb.Parser.of(Person.class), temp);
    }

    @Test
    @SuppressWarnings("null")
    public void testParserOfContext() throws Exception {
        assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Parser.of((JAXBContext) null));

        assertParserCompliance(Jaxb.Parser.of(JAXBContext.newInstance(Person.class)), temp);
    }

    @Test
    @SuppressWarnings("null")
    public void testParserBuilder() throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Parser.builder().build());

        assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Parser.builder().factory(null).build());

        assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Parser.builder().factory(validUnmarshaller).xxeFactory(null).build());

        for (boolean ignoreXXE : BOOLS) {
            assertParserCompliance(
                    Jaxb.Parser.<Person>builder()
                            .factory(validUnmarshaller)
                            .xxeFactory(validXxeFactory)
                            .ignoreXXE(ignoreXXE)
                            .build(),
                    temp);
        }
    }

    @Test
    @SuppressWarnings("null")
    public void testParserWither() throws IOException {
        Jaxb.Parser<Person> parser = Jaxb.Parser.of(Person.class);

        assertThatNullPointerException()
                .isThrownBy(() -> parser.withFactory(null));

        assertThatNullPointerException()
                .isThrownBy(() -> parser.withXxeFactory(null));
    }

    @Test
    public void testParserSafety() throws IOException {
        List<Meta<IO.Supplier<Unmarshaller>>> factories = Meta.<IO.Supplier<Unmarshaller>>builder()
                .valid("Ok", validUnmarshaller)
                .invalid("Null", IO.Supplier.of(null))
                .invalid("Throwing", IO.Supplier.throwing(IOError::new))
                .invalid("Checked", validUnmarshaller.andThen(o -> new ForwardingUnmarshaller(o).onUnmarshal(JaxbListener.checked(JaxbError::new))))
                .invalid("Unchecked", validUnmarshaller.andThen(o -> new ForwardingUnmarshaller(o).onUnmarshal(JaxbListener.unchecked(UncheckedError::new))))
                .build();

        for (boolean xxe : BOOLS) {

            List<Meta<IO.Supplier<XMLInputFactory>>> xxeFactories = Meta.<IO.Supplier<XMLInputFactory>>builder()
                    .valid("Ok", validXxeFactory)
                    .of("Null", xxe, IO.Supplier.of(null))
                    .of("Throwing", xxe, IO.Supplier.throwing(IOError::new))
                    .of("Checked", xxe, validXxeFactory.andThen(o -> new ForwardingXMLInputFactory(o).onCreate(StaxListener.checked(StaxError::new))))
                    .of("Unchecked", xxe, validXxeFactory.andThen(o -> new ForwardingXMLInputFactory(o).onCreate(StaxListener.unchecked(UncheckedError::new))))
                    .build();

            for (Meta<IO.Supplier<Unmarshaller>> factory : factories) {
                for (Meta<IO.Supplier<XMLInputFactory>> xxeFactory : xxeFactories) {

                    Xml.Parser<Person> parser = Jaxb.Parser
                            .<Person>builder()
                            .factory(factory.getTarget())
                            .ignoreXXE(!xxe)
                            .xxeFactory(xxeFactory.getTarget())
                            .build();

                    assertParserSafety(parser, Meta.lookupExpectedException(factory, xxeFactory));
                }
            }
        }
    }

    @Test
    @SuppressWarnings("null")
    public void testFormatterOfClass() throws Exception {
        assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Formatter.of((Class<?>) null));

        assertFormatterCompliance(Jaxb.Formatter.of(Person.class), false, temp);
    }

    @Test
    @SuppressWarnings("null")
    public void testFormatterOfContext() throws Exception {
        assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Formatter.of((JAXBContext) null));

        assertFormatterCompliance(Jaxb.Formatter.of(JAXBContext.newInstance(Person.class)), false, temp);
    }

    @Test
    @SuppressWarnings("null")
    public void testFormatterBuilder() throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Formatter.builder().build());

        assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Formatter.builder().factory(null).build());

        for (boolean formatted : BOOLS) {
            assertFormatterCompliance(
                    Jaxb.Formatter.<Person>builder()
                            .factory(validMarshaller)
                            .formatted(formatted)
                            .build(),
                    formatted, temp);
        }
    }

    @Test
    @SuppressWarnings("null")
    public void testFormatterWither() throws IOException {
        Jaxb.Formatter<Person> formatter = Jaxb.Formatter.of(Person.class);

        assertThatNullPointerException()
                .isThrownBy(() -> formatter.withFactory(null));

        assertThatNullPointerException()
                .isThrownBy(() -> formatter.withEncoding(null));
    }

    @Test
    @SuppressWarnings("null")
    public void testFormatterWithAlternateEncoding() throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Formatter.of(Person.class).withEncoding(null));

        assertThat(Jaxb.Formatter.of(Person.class).withEncoding(StandardCharsets.ISO_8859_1).formatToString(Person.JOHN_DOE))
                .isEqualTo(Person.JOHN_DOE_CHARS.replace("UTF-8", "ISO-8859-1"));
    }

    @Test
    public void testFormatterSafety() throws IOException {
        List<Meta<IO.Supplier<Marshaller>>> factories = Meta.<IO.Supplier<Marshaller>>builder()
                .valid("Ok", validMarshaller)
                .invalid("Null", IO.Supplier.of(null))
                .invalid("Throwing", IO.Supplier.throwing(IOError::new))
                .invalid("Checked", validMarshaller.andThen(o -> new ForwardingMarshaller(o).onMarshal(JaxbListener.checked(JaxbError::new))))
                .invalid("Unchecked", validMarshaller.andThen(o -> new ForwardingMarshaller(o).onMarshal(JaxbListener.unchecked(UncheckedError::new))))
                .build();

        for (boolean formatted : BOOLS) {
            for (Charset encoding : ENCODINGS) {
                for (Meta<IO.Supplier<Marshaller>> factory : factories) {

                    Xml.Formatter<Person> formatter = Jaxb.Formatter
                            .<Person>builder()
                            .factory(factory.getTarget())
                            .formatted(formatted)
                            .encoding(encoding)
                            .build();

                    assertFormatterSafety(formatter, Meta.lookupExpectedException(factory), temp);
                }
            }
        }
    }

    private static final class IOError extends IOException {
    }

    private static final class UncheckedError extends RuntimeException {
    }

    private static final class JaxbError extends JAXBException {

        public JaxbError() {
            super("");
        }
    }

    private static final class StaxError extends XMLStreamException {
    }
}
