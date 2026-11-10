import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../api/cliente'
import { pacientesApi } from '../api/pacientesApi'
import Campo from '../components/forms/Campo'
import FechaNacimientoInput from '../components/forms/FechaNacimientoInput'
import Cargando from '../components/ui/Cargando'
import EncabezadoPagina from '../components/ui/EncabezadoPagina'
import ErrorCarga from '../components/ui/ErrorCarga'
import { useAvisos } from '../context/AvisosContext'
import { useConsulta } from '../hooks/useConsulta'
import type { PacienteRequest } from '../types'
import { validarPaciente } from '../utils/validaciones'

const VACIO: PacienteRequest = { nombre: '', apellido: '', dni: '', telefono: '', direccion: '', fechaNacimiento: '' }

/*
 Alta y edición de un paciente (rutas /pacientes/nuevo y /pacientes/:idPaciente/editar).
 */
export default function PacienteForm() {
  const { idPaciente } = useParams()
  const navigate = useNavigate()
  const { mostrarAviso } = useAvisos()

  const editando = idPaciente !== undefined

  // al editar, primero se trae el paciente; al crear no hay nada que cargar
  const { datos: existente, cargando, error, recargar } = useConsulta(
    () => (editando ? pacientesApi.obtener(Number(idPaciente)) : Promise.resolve(null)),
    [idPaciente],
  )

  const [form, setForm] = useState<PacienteRequest>(VACIO)
  // la fecha de nacimiento tiene su propio mensaje de error porque el componente conoce mejor qué falló (día, mes o año)
  const [errorFecha, setErrorFecha] = useState<string | null>(null)
  const [intentoEnviar, setIntentoEnviar] = useState(false)
  const [errorServidor, setErrorServidor] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)

  // cuando llega el paciente a editar se vuelcan sus datos en el formulario
  useEffect(() => {
    if (existente) {
      const { nombre, apellido, dni, telefono, direccion, fechaNacimiento } = existente
      setForm({ nombre, apellido, dni, telefono, direccion, fechaNacimiento })
    }
  }, [existente])

  // actualiza un solo campo sin tocar los demás. Devuelve el manejador de onChange de ese campo
  const cambiar = (campo: keyof PacienteRequest) => (e: { target: { value: string } }) => setForm((f) => ({ ...f, [campo]: e.target.value }))

  const errores = validarPaciente(form)
  // si el componente de fecha detectó un error puntual, ese mensaje pisa al genérico
  if (errorFecha) errores.fechaNacimiento = errorFecha
  const mostrar = (campo: keyof PacienteRequest) => (intentoEnviar ? errores[campo] : undefined)
  const clase = (campo: keyof PacienteRequest) => `form-control ${mostrar(campo) ? 'is-invalid' : ''}`

  const guardar = async (evento: FormEvent) => {
    evento.preventDefault()
    setIntentoEnviar(true)
    setErrorServidor(null)
    if (Object.keys(errores).length > 0) return

    setGuardando(true)
    try {
      // se limpian los espacios sobrantes antes de enviar
      const cuerpo: PacienteRequest = {
        ...form,
        nombre: form.nombre.trim(),
        apellido: form.apellido.trim(),
        dni: form.dni.trim(),
        telefono: form.telefono.trim(),
        direccion: form.direccion.trim(),
      }
      const guardado = editando ? await pacientesApi.editar(Number(idPaciente), cuerpo) : await pacientesApi.crear(cuerpo)

      mostrarAviso('exito', editando ? 'Paciente actualizado correctamente.' : 'Paciente registrado correctamente.')
      navigate(editando ? '/pacientes' : `/pacientes/${guardado.idPaciente}`)
    } catch (e) {
      // por ejemplo: 409 "Ya existe un paciente con el DNI ..."
      setErrorServidor(e instanceof ApiError ? e.message : 'Ocurrió un error inesperado al guardar el paciente.')
    } finally {
      setGuardando(false)
    }
  }

  if (cargando && editando && !existente) return <Cargando texto="Cargando paciente..." />
  if (error) return <ErrorCarga mensaje={error} alReintentar={recargar} />

  return (
    <>
      <EncabezadoPagina titulo={editando ? 'Editar paciente' : 'Nuevo paciente'} />

      <div className="row">
        <div className="col-xl-8 col-lg-10">
          <div className="card shadow mb-4">
            <div className="card-header py-3">
              <h6 className="m-0 font-weight-bold text-primary">Datos personales</h6>
            </div>
            <div className="card-body">
              {errorServidor && (
                <div className="alert alert-danger" role="alert">
                  <i className="fas fa-exclamation-triangle mr-2"></i>
                  {errorServidor}
                </div>
              )}

              <form onSubmit={guardar} noValidate>
                <div className="form-row">
                  <div className="col-md-6">
                    <Campo etiqueta="Nombre" htmlFor="nombre" obligatorio error={mostrar('nombre')}>
                      <input id="nombre" className={clase('nombre')} value={form.nombre} onChange={cambiar('nombre')} maxLength={60} autoComplete="off" />
                    </Campo>
                  </div>
                  <div className="col-md-6">
                    <Campo etiqueta="Apellido" htmlFor="apellido" obligatorio error={mostrar('apellido')}>
                      <input id="apellido" className={clase('apellido')} value={form.apellido} onChange={cambiar('apellido')} maxLength={60} autoComplete="off" />
                    </Campo>
                  </div>
                </div>

                <div className="form-row">
                  <div className="col-md-6">
                    <Campo etiqueta="DNI" htmlFor="dni" obligatorio error={mostrar('dni')}>
                      <input id="dni" className={clase('dni')} value={form.dni} onChange={cambiar('dni')} inputMode="numeric" maxLength={9} placeholder="Sin puntos" autoComplete="off" />
                    </Campo>
                  </div>
                  <div className="col-md-6">
                    <Campo etiqueta="Fecha de nacimiento" htmlFor="fechaNacimiento" obligatorio error={mostrar('fechaNacimiento')}>
                      <FechaNacimientoInput
                        id="fechaNacimiento"
                        valor={form.fechaNacimiento}
                        alCambiar={(fecha, errorDeFecha) => {
                          setForm((f) => ({ ...f, fechaNacimiento: fecha }))
                          setErrorFecha(errorDeFecha)
                        }}
                        hayError={Boolean(mostrar('fechaNacimiento'))}
                      />
                    </Campo>
                  </div>
                </div>

                <div className="form-row">
                  <div className="col-md-6">
                    <Campo etiqueta="Teléfono" htmlFor="telefono" obligatorio error={mostrar('telefono')}>
                      <input id="telefono" className={clase('telefono')} value={form.telefono} onChange={cambiar('telefono')} inputMode="tel" maxLength={25} placeholder="Ej: 223 555-0101" autoComplete="off" />
                    </Campo>
                  </div>
                  <div className="col-md-6">
                    <Campo etiqueta="Dirección" htmlFor="direccion" obligatorio error={mostrar('direccion')}>
                      <input id="direccion" className={clase('direccion')} value={form.direccion} onChange={cambiar('direccion')} maxLength={120} autoComplete="off" />
                    </Campo>
                  </div>
                </div>

                <div className="mt-3">
                  <button type="submit" className="btn btn-primary mr-2" disabled={guardando}>
                    <i className="fas fa-save mr-1"></i> {guardando ? 'Guardando...' : editando ? 'Guardar cambios' : 'Registrar paciente'}
                  </button>
                  <Link to="/pacientes" className="btn btn-secondary">
                    Cancelar
                  </Link>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </>
  )
}
