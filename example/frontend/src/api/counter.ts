import { graphql } from 'src/gql'

export const CounterSubscription = graphql(/* GraphQL */ `
    subscription Counter {
       counter
    }
`);
