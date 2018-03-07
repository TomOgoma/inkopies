package ke.co.definition.inkopies.presentation.verification

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.databinding.Observable
import android.databinding.ObservableField
import android.support.design.widget.Snackbar
import ke.co.definition.inkopies.R
import ke.co.definition.inkopies.model.ResourceManager
import ke.co.definition.inkopies.model.auth.Authable
import ke.co.definition.inkopies.model.auth.OTPStatus
import ke.co.definition.inkopies.model.auth.VerifLogin
import ke.co.definition.inkopies.presentation.common.ProgressData
import ke.co.definition.inkopies.presentation.common.ResIDSnackBarData
import ke.co.definition.inkopies.presentation.common.SnackBarData
import ke.co.definition.inkopies.presentation.common.TextSnackBarData
import ke.co.definition.inkopies.utils.injection.Dagger2Module
import ke.co.definition.inkopies.utils.livedata.SingleLiveEvent
import rx.Scheduler
import rx.Subscription
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Named


/**
 * Created by tomogoma
 * On 02/03/18.
 */
class VerificationViewModel @Inject constructor(
        private val auth: Authable,
        @Named(Dagger2Module.SCHEDULER_IO) private val subscribeOnScheduler: Scheduler,
        @Named(Dagger2Module.SCHEDULER_MAIN_THREAD) private val observeOnScheduler: Scheduler,
        private val resMngr: ResourceManager
) : ViewModel() {

    val openEditDialog: SingleLiveEvent<Boolean> = SingleLiveEvent()
    val finishedEv: SingleLiveEvent<Boolean> = SingleLiveEvent()
    val finishedChangeIdentifierEv: SingleLiveEvent<Boolean> = SingleLiveEvent()
    val snackBarData: SingleLiveEvent<SnackBarData> = SingleLiveEvent()

    val resetCDTimer: ObservableField<String> = ObservableField()
    val progress: ObservableField<ProgressData> = ObservableField()
    val verifLogin: ObservableField<VerifLogin> = ObservableField()
    val otp: ObservableField<String> = ObservableField()
    val updatedIdentifier: ObservableField<String> = ObservableField()
    val updatedIdentifierErr: ObservableField<String> = ObservableField()

    private val hasBeenStarted = AtomicBoolean()
    private var resetCDSub: Subscription? = null

    init {
        updatedIdentifier.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(p0: Observable?, p1: Int) {
                val err = updatedIdentifierErr.get()
                if (err != null && !err.isEmpty()) {
                    updatedIdentifierErr.set("")
                }
            }
        })
    }

    fun start(vl: VerifLogin) {

        verifLogin.set(vl)

        if (vl.verified) {
            onClaimVerified()
            return
        }

        if (vl.otpStatus != null && vl.otpStatus.isExpired()) {
            onRequestResendOTP()
            return
        }

        if (hasBeenStarted.compareAndSet(false, true)) {
            countDownResetVisibility()
        }
    }

    fun onSubmit() {

        auth.verifyOTP(verifLogin.get(), otp.get())
                .doOnSubscribe {
                    progress.set(ProgressData(String.format(
                            resMngr.getString(R.string.verifying_ss), verifLogin.get().value)))
                }
                .doOnUnsubscribe { progress.set(ProgressData()) }
                .subscribeOn(subscribeOnScheduler)
                .observeOn(observeOnScheduler)
                .subscribe({
                    finishedEv.value = true
                }, {
                    snackBarData.value = TextSnackBarData(it.message!!, Snackbar.LENGTH_LONG)
                })
    }

    fun onSubmitChangeIdentifier() {
        val id = updatedIdentifier.get()
        if (id == null || id.isEmpty()) {
            updatedIdentifierErr.set(resMngr.getString(R.string.error_required_field))
            return
        }
        auth.updateIdentifier(updatedIdentifier.get())
                .doOnSubscribe {
                    progress.set(ProgressData(String.format(
                            resMngr.getString(R.string.updating_login_details_to_ss), id)))
                }
                .doOnUnsubscribe { progress.set(ProgressData()) }
                .subscribeOn(subscribeOnScheduler)
                .observeOn(observeOnScheduler)
                .subscribe({ onIdentifierUpdated(it) }, {
                    snackBarData.value = TextSnackBarData(it.message!!, Snackbar.LENGTH_LONG)
                })
    }

    fun onRequestResendOTP() {

        auth.sendVerifyOTP(verifLogin.get())
                .doOnSubscribe {
                    progress.set(ProgressData(resMngr.getString(
                            R.string.sending_verification_code)))
                }
                .doOnUnsubscribe { progress.set(ProgressData()) }
                .subscribeOn(subscribeOnScheduler)
                .observeOn(observeOnScheduler)
                .subscribe({ onOTPResent(it) }, {
                    snackBarData.value = TextSnackBarData(it.message!!, Snackbar.LENGTH_LONG)
                })
    }

    fun onClaimVerified() {

        auth.checkIdentifierVerified(verifLogin.get())
                .doOnSubscribe {
                    progress.set(ProgressData(String.format(resMngr.getString(
                            R.string.checking_ss_verified), verifLogin.get().value)))
                }
                .doOnUnsubscribe { progress.set(ProgressData()) }
                .subscribeOn(subscribeOnScheduler)
                .observeOn(observeOnScheduler)
                .subscribe({
                    finishedEv.value = true
                }, {
                    snackBarData.value = TextSnackBarData(it.message!!, Snackbar.LENGTH_LONG)
                })
    }

    private fun onOTPResent(it: OTPStatus) {
        val vl = verifLogin.get()
        hasBeenStarted.set(false)
        start(VerifLogin(vl.id, vl.userID, vl.value, vl.verified, it))
        snackBarData.value = ResIDSnackBarData(R.string.verification_code_resent, Snackbar.LENGTH_LONG)
    }

    private fun onIdentifierUpdated(newVL: VerifLogin) {
        hasBeenStarted.set(false)
        start(newVL)
        finishedChangeIdentifierEv.value = true
    }

    private fun countDownResetVisibility() {
        resetCDSub?.unsubscribe()
        resetCDSub = auth.resendInterval(verifLogin.get().otpStatus, 1)
                .doOnUnsubscribe({ resetCDTimer.set("") })
                .subscribeOn(subscribeOnScheduler)
                .observeOn(observeOnScheduler)
                .subscribe({ resetCDTimer.set(it) })
    }

    class Factory @Inject constructor(
            private val auth: Authable,
            @Named(Dagger2Module.SCHEDULER_IO) private val subscribeOnScheduler: Scheduler,
            @Named(Dagger2Module.SCHEDULER_MAIN_THREAD) private val observeOnScheduler: Scheduler,
            private val resMngr: ResourceManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return VerificationViewModel(auth, subscribeOnScheduler, observeOnScheduler, resMngr) as T
        }

    }
}