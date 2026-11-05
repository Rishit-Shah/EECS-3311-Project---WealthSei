package wealthsei.importing;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import wealthsei.domain.ParseResult;

/**
 * Reads a CSV file, works out which bank layout it uses, and returns its transactions.
 * New layouts are supported by adding another adapter to the list.
 */
public class CsvImporter {

    private final List<BiFunction<CsvParser, Supplier<String>, BankCsvAdapter>> adapterFactories;
    private final Supplier<String> idSupplier;

    public CsvImporter() {
        this(() -> UUID.randomUUID().toString());
    }

    /** @param idSupplier produces the id of each new transaction (tests pass a counter) */
    public CsvImporter(Supplier<String> idSupplier) {
        this.idSupplier = Objects.requireNonNull(idSupplier, "idSupplier");
        this.adapterFactories = List.of(CardStatementCsvAdapter::new, SignedAmountCsvAdapter::new, DebitCreditCsvAdapter::new);
    }

    /**
     * @throws UnsupportedFormatException if the file cannot be read, is empty, or matches no layout
     */
    public ParseResult parse(Path path) {
        CsvParser parser;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            parser = CsvParser.parse(reader);
        } catch (IOException e) {
            throw new UnsupportedFormatException("Could not read file " + path.getFileName()
                    + " as UTF-8 text: " + e.getMessage(), e);
        } catch (CsvFormatException e) {
            throw new UnsupportedFormatException("Not a valid CSV file: " + e.getMessage(), e);
        }
        if (parser.header().isEmpty()) {
            throw new UnsupportedFormatException("The file is empty");
        }
        return detectAdapter(parser.header(), parser).readTransactions();
    }

    /** The first registered adapter whose layout matches the header. */
    public BankCsvAdapter detectAdapter(List<String> header, CsvParser parser) {
        StringBuilder supported = new StringBuilder();
        for (var factory : adapterFactories) {
            BankCsvAdapter candidate = factory.apply(parser, idSupplier);
            if (candidate.canHandle(header)) {
                return candidate;
            }
            if (supported.length() > 0) {
                supported.append("; ");
            }
            supported.append(candidate.layoutDescription());
        }
        throw new UnsupportedFormatException("Unsupported CSV layout. Header was " + header
                + ". Supported layouts: " + supported);
    }
}
