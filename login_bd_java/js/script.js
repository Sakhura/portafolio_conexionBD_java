const URL_BACKEND = 'http://localhost:8080/api/login';

const formulario = document.getElementById('formLogin');
const mensajeEl = document.getElementById('mensaje');

formulario.addEventListener('submit', async (evento) => {
  evento.preventDefault();

  const correo = document.getElementById('correo').value.trim();
  const password = document.getElementById('password').value;
  const boton = formulario.querySelector('.boton');

  mostrarMensaje('Verificando...', null);
  boton.disabled = true;

  try {
    const respuesta = await fetch(URL_BACKEND, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({ correo, password }),
    });

    const datos = await respuesta.json();
    mostrarMensaje(datos.mensaje, datos.ok);

    if (datos.ok) {
      formulario.reset();
    }
  } catch (error) {
    mostrarMensaje('No se pudo conectar con el servidor. ¿Esta corriendo el backend?', false);
  } finally {
    boton.disabled = false;
  }
});

function mostrarMensaje(texto, ok) {
  mensajeEl.textContent = texto;
  mensajeEl.classList.remove('error', 'exito');
  if (ok === true) mensajeEl.classList.add('exito');
  if (ok === false) mensajeEl.classList.add('error');
}
