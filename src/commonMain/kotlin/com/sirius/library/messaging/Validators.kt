package com.sirius.library.messaging

import com.sirius.library.errors.sirius_exceptions.SiriusValidationError
import com.sirius.library.utils.JSONObject
import kotlinx.serialization.json.JsonObject

class Validators {

    @Throws(SiriusValidationError::class)
    fun checkForAttributes(partial: JSONObject?, vararg expectedAttributes: String?) {
        for (attribute in expectedAttributes) {
            //    throw new SiriusValidationError(String.format("Attribute %s is missing from message: \n%s",attribute, partial.toString()));
            /*
            if isinstance(attribute, tuple):
            if attribute[0] not in partial:
            raise SiriusValidationError ('Attribute "{}" is missing from message: \n{}'.format(attribute[0], partial))
            if partial[attribute[0]] != attribute[1]:
            raise SiriusValidationError
            ('Message.{}: {} != {}'.format(attribute[0], partial[attribute[0]], attribute[1]))
            else:
            if attribute not in partial:
            raise SiriusValidationError ('Attribute "{}" is missing from message: \n{}'.format(attribute, partial))*/
        }
    }

    /**
     * Validate blocks of message like threading, timing, etc
     */
    @Throws(SiriusValidationError::class)
    fun validateCommonBlocks(partial: JSONObject) {
        validateThreadBlock(partial)
        validateTimingBlock(partial)
    }

    @Throws(SiriusValidationError::class)
    fun validateThreadBlock(partial: JSONObject) {
        if (partial.has(THREAD_DECORATOR)) {
            val thread: JSONObject? = partial.getJSONObject(THREAD_DECORATOR)
            checkForAttributes(thread, THREAD_ID)
            val thread_id: String? = thread?.getString(THREAD_ID)

            /* if partial.get(ID) and thread_id ==partial[ID]:
        raise SiriusValidationError ('Thread id {} cannot be equal to outer id {}'.format(thread_id, partial[ID]))
        if thread.get(PARENT_THREAD_ID) and thread[ PARENT_THREAD_ID]in(thread_id, partial[ID]):
        raise SiriusValidationError ('Parent thread id {} must be different than thread id and outer id'.format(
                thread[PARENT_THREAD_ID]))

        if thread.get(SENDER_ORDER):
        non_neg_num = NonNegativeNumberField()
        err = non_neg_num.validate(thread[SENDER_ORDER])
        if not err:
        if RECEIVED_ORDERS in thread and thread[RECEIVED_ORDERS]:
        recv_ords = thread[RECEIVED_ORDERS]
        err = MapField(DIDField(), non_neg_num).validate(recv_ords)
        if err:
        raise ValueError (err)*/
        }
    }

    fun validateTimingBlock(partial: JSONObject) {
        if (partial.has(Companion.TIMING_DECORATOR)) {
            val timing: JSONObject? = partial.getJSONObject(Companion.TIMING_DECORATOR)

            /*   non_neg_num = NonNegativeNumberField()
            iso_data = ISODatetimeStringField()
            expected_iso_fields = [IN_TIME, OUT_TIME, STALE_TIME, EXPIRES_TIME, WAIT_UNTIL_TIME]
            for f in expected_iso_fields:
            if f in timing:
            err = iso_data.validate(timing[f])
            if err:
            raise SiriusValidationError (err)
            if DELAY_MILLI in timing:
            err = non_neg_num.validate(timing[DELAY_MILLI])
            if err:
            raise SiriusValidationError (err)*/

            //In time cannot be greater than out time
            /*     if IN_TIME in timing and OUT_TIME in timing:
            t_in = iso_data.parse_func(timing[IN_TIME])
            t_out = iso_data.parse_func(timing[OUT_TIME])

            if t_in > t_out:
            raise SiriusValidationError ('{} cannot be greater than {}'.format(IN_TIME, OUT_TIME))

            #Stale time cannot be greater than expires time
            if STALE_TIME in timing and EXPIRES_TIME in timing:
            t_stale = iso_data.parse_func(timing[STALE_TIME])
            t_exp = iso_data.parse_func(timing[EXPIRES_TIME])

            if t_stale > t_exp:
            raise SiriusValidationError ('{} cannot be greater than {}'.format(STALE_TIME, EXPIRES_TIME))*/
        }
    }

    companion object {
        const val ID = "@id"
        const val TYPE = "@type"
        const val THREAD_DECORATOR = "~thread"
        const val THREAD_ID = "thid"
        const val PARENT_THREAD_ID = "pthid"
        const val SENDER_ORDER = "sender_order"
        const val RECEIVED_ORDERS = "received_orders"
        const val THREADING_ERROR = "threading_error"
        const val TIMING_ERROR = "timing_error"
        const val IN_TIME = "in_time"
        const val OUT_TIME = "out_time"
        const val STALE_TIME = "stale_time"
        const val EXPIRES_TIME = "expires_time"
        const val DELAY_MILLI = "delay_milli"
        const val WAIT_UNTIL_TIME = "wait_until_time"
        const val  TIMING_DECORATOR = "~timing"
    }
}
