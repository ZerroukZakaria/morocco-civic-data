package io.github.zerroukzakaria.civicdata;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI civicDataOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Morocco Civic Data API")
                .version("0.1.0")
                .description("""
                        Read-only API for Moroccan election results, down to the constituency level.

                        Voters cast two ballots, one for a local constituency and one for a regional list, so \
                        results are always reported per ballot and shares are never mixed across them.

                        Shares are percentages of the votes cast for lists. Blank and spoilt votes are not \
                        available in the source data. Turnout rates are fractions between 0 and 1, as given \
                        by the source.

                        Data: Association Tafra, "Résultats des élections législatives marocaines de 2021, \
                        listes locales et régionales" (2022), licensed CC BY 4.0, copied by Tafra from \
                        elections.ma. Every results response carries its own attribution block."""));
    }
}