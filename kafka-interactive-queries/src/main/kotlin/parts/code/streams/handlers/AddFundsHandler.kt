package parts.code.streams.handlers

import java.math.BigDecimal
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import parts.code.interactive.queries.schemas.FundsAdded
import parts.code.streams.config.KafkaConfig
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.http.Status

class AddFundsHandler @Inject constructor(
    private val clock: Clock,
    private val config: KafkaConfig,
    private val producer: KafkaProducer<String, SpecificRecord>
) : Handler {

    private val logger = LoggerFactory.getLogger(AddFundsHandler::class.java)

    override fun handle(ctx: Context) {
        ctx.parse(AddFundsPayload::class.java).then {
            val record = FundsAdded(
                UUID.randomUUID().toString(),
                clock.instant(),
                it.customerId,
                it.amount
            )

            val producerRecord = ProducerRecord(
                config.topics.balance,
                it.customerId,
                record as SpecificRecord
            )

            producer.send(producerRecord).get()

            logger.info(
                "Sent ${record.schema.name} to topic: ${config.topics.balance}\n\trecord: $record"
            )

            ctx.response.status(Status.ACCEPTED)
            ctx.render("")
        }
    }

    private data class AddFundsPayload(val customerId: String, val amount: BigDecimal)
}
