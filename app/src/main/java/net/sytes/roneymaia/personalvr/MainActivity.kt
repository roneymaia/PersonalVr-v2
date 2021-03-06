package net.sytes.roneymaia.personalvr

import android.animation.ValueAnimator
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast

import android.util.Log
import android.view.View
import com.facebook.CallbackManager
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.FacebookCallback
import com.facebook.login.widget.LoginButton
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FacebookAuthProvider
import com.facebook.AccessToken
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.SignInButton
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private var mAuth: FirebaseAuth? = null
    private var loginButton: LoginButton? = null
    private var mCallbackManager: CallbackManager? = null
    private var customFbButton: Button? = null
    private var mGoogleSignInClient: GoogleSignInClient? = null
    private var googleButton: SignInButton? = null
    private var customGoogleButton: Button? = null
    private var viewCanvas: View? = null
    private var btnEntrar: Button? = null
    private var txtEmail: EditText? = null
    private var txtSenha: EditText? = null
    private var animation: ValueAnimator? = null
    private var metricsMain: DisplayMetrics? = null
    private var viewFrag: View? = null
    private var btnCadastrar: Button? = null
    private var animationArrow: ValueAnimator? = null
    private var arrowFrag: ImageView? = null
    private var nIt: Int? = 0
    private var firebaseDb: FirebaseDatabase? = null

    companion object {
        const val PVR_CODE = 1
        fun isNullOrEmpty(str: String?): Boolean{
            if(str != null && !str.isEmpty()){
                return false
            }
            return true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        metricsMain = resources.displayMetrics

        viewCanvas = findViewById<CustomViewCanvas>(R.id.viewCanvas) // canvas view customizada

        mAuth = FirebaseAuth.getInstance() // obtem a instancia de autenticacao
        firebaseDb = FirebaseDatabase.getInstance() // obtem a instancia de autenticacao

        if(mAuth!!.currentUser != null){
            startActivity(Intent(MainActivity@this, MapsActivity::class.java))
        }else{
            removeAuths() // Remove autenticacoes por prevencao
        }

        // ##### Firebase AUTH #####

        mCallbackManager = CallbackManager.Factory.create()

        // ##### LoginButton Facebook #####
        loginButton = findViewById<View>(R.id.facebookLoginButton) as LoginButton
        loginButton!!.setReadPermissions("email")

        // Callback registration
        loginButton!!.registerCallback(mCallbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                signInFacebook(loginResult.accessToken)
            }

            override fun onCancel() {
            }

            override fun onError(exception: FacebookException) {
                Toast.makeText(applicationContext, "Falha ao autenticar com o Facebook.", Toast.LENGTH_SHORT).show()
            }
        })

        // ##### Google AUTH #####
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this@MainActivity, gso)

        googleButton = findViewById<View>(R.id.googleLoginButton) as SignInButton
        googleButton!!.setOnClickListener { _ ->
            val signInIntent = this@MainActivity.mGoogleSignInClient!!.signInIntent
            startActivityForResult(signInIntent, PVR_CODE) }

        // ##### CustomFacebook button login #####
        customFbButton = findViewById<View>(R.id.customFacebookButton) as Button
        customFbButton!!.setOnClickListener { _ ->
            this@MainActivity.loginButton!!.performClick() }

        // ##### CustomGoogleButton #####
        customGoogleButton = findViewById<View>(R.id.customGoogleButton) as Button
        customGoogleButton!!.setOnClickListener { _ ->
            for (i: Int in 0..googleButton!!.childCount){
                var view: View = this@MainActivity.googleButton!!.getChildAt(i)

                if(view is Button){
                    view.performClick()
                    return@setOnClickListener
                }
            }

        }

        txtEmail = findViewById<View>(R.id.txtEmail) as EditText
        txtEmail!!.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {

                if(this@MainActivity.nIt!! >= 163){
                  this@MainActivity.nIt = 0
                }else{
                    val nIni = this@MainActivity.nIt!!
                    var nFim: Int = this@MainActivity.nIt!! + 4

                    if(nFim >= 163){
                        nFim = 163
                    }
                    for (number in nIni..nFim){
                        for (numberloop in 0..10){
                            SingletonControlCanvas.imageNumber = number
                            this@MainActivity.viewCanvas!!.invalidate()
                        }

                    }
                    this@MainActivity.nIt = nFim
                }


            }

        })
        txtSenha = findViewById<View>(R.id.txtSenha) as EditText
        txtSenha!!.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {

                if(this@MainActivity.nIt!! >= 163){
                    this@MainActivity.nIt = 0
                }else{
                    val nIni = this@MainActivity.nIt!!
                    var nFim: Int = this@MainActivity.nIt!! + 4

                    if(nFim >= 163){
                        nFim = 163
                    }
                    for (number in nIni..nFim){
                        SingletonControlCanvas.imageNumber = number
                        this@MainActivity.viewCanvas!!.invalidate()
                    }
                    this@MainActivity.nIt = nFim
                }


            }

        })

        // Evento de click no botão de entrar
        btnEntrar = findViewById<View>(R.id.btnEntrar) as Button
        btnEntrar!!.setOnClickListener { _ ->
            val email = this@MainActivity.txtEmail!!.text!!.toString()
            val senha = this@MainActivity.txtSenha!!.text!!.toString()

            if(!(isNullOrEmpty(email) || isNullOrEmpty(senha))){
                signInEmailAndPassword(email, senha)
            }

        }

        // Obtem a view do fragmento
        viewFrag = supportFragmentManager!!.findFragmentById(R.id.mainFragment).view
        arrowFrag = viewFrag!!.findViewById(R.id.arrowFrag) as ImageView

        //Animação da flecha
        animationArrow = ValueAnimator.ofFloat( 0f, 180f)
        animationArrow!!.duration = 1000
        animationArrow!!.addUpdateListener { animation: ValueAnimator? ->
            arrowFrag!!.rotation = animation!!.animatedValue as Float
        }

        btnCadastrar = findViewById<View>(R.id.buttonCadastrar) as Button
        btnCadastrar!!.setOnClickListener { _ ->

            this@MainActivity.animation!!.start()

        }

        // Objeto de animação
        animation = ValueAnimator.ofFloat(metricsMain!!.heightPixels.toFloat(), 0f)
        animation!!.duration = 1500
        animation!!.addUpdateListener { animation: ValueAnimator? ->
            this@MainActivity.viewFrag!!.translationY = animation!!.animatedValue as Float
            if((animation!!.animatedValue as Float) == 0f){
                this@MainActivity.animationArrow!!.start()
            }
        }

    }

    private fun signInEmailAndPassword(email: String, password: String) : Boolean{

        var success = false

        this@MainActivity.mAuth?.signInWithEmailAndPassword(email, password)?.addOnCompleteListener(this@MainActivity) { task ->
            if (task.isSuccessful) {
                success = true
                startMaps()
            } else {
                Toast.makeText(this@MainActivity, "Usuário ou senha incorretos.", Toast.LENGTH_SHORT).show()
                success = false
            }
        }
        return success
    }

    private fun signInFacebook(token: AccessToken) {

        Log.d("AcessToken", "handleFacebookAccessToken:$token")

        val credential = FacebookAuthProvider.getCredential(token.token)
        this@MainActivity.mAuth?.signInWithCredential(credential)?.addOnCompleteListener(this@MainActivity) { task ->
            if (task.isSuccessful) {
                val user: FirebaseUser = mAuth!!.currentUser!!
                startMaps()
            } else {
                Log.d("FacebookLogin", "signInWithEmail:failure")
                Toast.makeText(this@MainActivity, "Falha ao autenticar com o Facebook.",
                        Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        this@MainActivity.mAuth!!.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user: FirebaseUser? = mAuth!!.currentUser
                        startMaps()
                    } else {
                        Toast.makeText(this@MainActivity, "Falha ao autenticar com o Google.",
                                Toast.LENGTH_SHORT).show()
                    }
                }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode === MainActivity.PVR_CODE) {

            val result: GoogleSignInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess) {
                val account: GoogleSignInAccount = result.signInAccount!!
                firebaseAuthWithGoogle(account)
            } else {
                Toast.makeText(this@MainActivity, "Falha ao autenticar com o Google.",
                        Toast.LENGTH_SHORT).show()
            }
        }else{
            this@MainActivity.mCallbackManager!!.onActivityResult(requestCode, resultCode, data)
        }

     }

    fun removeAuths() {
        // remove o login com facebook
        LoginManager.getInstance().logOut()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        // remove o login com google
        GoogleSignIn.getClient(this@MainActivity, gso).signOut()
        // remove os logins
        FirebaseAuth.getInstance().signOut()
    }

    fun startMaps() {

        val currentUser = MapsActivity@this.mAuth!!.currentUser

        SingletonControlCanvas.uid = currentUser?.uid
        SingletonControlCanvas.firebaseDb = MapsActivity@this.firebaseDb
        SingletonControlCanvas.contexto = MainActivity@this

        MapsActivity@this.firebaseDb!!
                .getReference("/markers/")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val status = dataSnapshot.hasChild(SingletonControlCanvas.uid)
                        if (!status) {
                            SingletonControlCanvas.firebaseDb!!
                                    .getReference("/markers/" + SingletonControlCanvas.uid)
                                    .setValue(UserMark(SingletonControlCanvas.uid, 0.0, 0.0, "on", ""))

                        }

                        startActivity(Intent(SingletonControlCanvas.contexto, MapsActivity::class.java))

                    }

                    override fun onCancelled(databaseError: DatabaseError) {

                    }
                })

    }

}
